package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.UserStatus;
import com.crm.dto.request.AdminInviteRequest;
import com.crm.exception.BadRequestException;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.InvitationInvalidException;
import com.crm.exception.LastAdminException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.exception.UserOperationForbiddenException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AdminUserManagementService {

    static final String ROLE_COMPANY_ADMIN = "ROLE_COMPANY_ADMIN";
    static final String ROLE_SUPER_ADMIN   = "ROLE_SUPER_ADMIN";
    static final String ROLE_ADMIN         = "ROLE_ADMIN";

    private static final Set<String> VALID_COMPANY_ROLES =
            Set.of("ROLE_COMPANY_ADMIN", "ROLE_USER", "ROLE_SALES", "ROLE_SUPPORT");

    private final UserRepository      userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder     passwordEncoder;
    private final OtpService          otpService;
    private final EmailService        emailService;

    public AdminUserManagementService(UserRepository userRepository,
                                      WorkspaceRepository workspaceRepository,
                                      PasswordEncoder passwordEncoder,
                                      OtpService otpService,
                                      EmailService emailService) {
        this.userRepository      = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder     = passwordEncoder;
        this.otpService          = otpService;
        this.emailService        = emailService;
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    /**
     * Creates a user with INVITED status and sends an OTP to their email.
     * If the user already exists as INVITED, resends the OTP.
     * Acting user must be COMPANY_ADMIN of the target workspace, or SUPER_ADMIN.
     */
    public void inviteUser(AdminInviteRequest request, User actingUser) {
        if (!VALID_COMPANY_ROLES.contains(request.role())) {
            throw new BadRequestException("Invalid role: " + request.role());
        }
        assertCanInviteTo(actingUser, request.workspaceId());

        String email = request.email().toLowerCase(Locale.ROOT);

        userRepository.findByEmailAndWorkspaceId(email, request.workspaceId()).ifPresent(existing -> {
            if (existing.getStatus() == UserStatus.ACTIVE) {
                throw new DuplicateEmailException(email);
            }
            if (existing.getStatus() == UserStatus.DISABLED) {
                throw new UserOperationForbiddenException("Cannot invite a disabled user");
            }
            // INVITED — fall through to resend OTP below
        });

        User invited = userRepository.findByEmailAndWorkspaceId(email, request.workspaceId())
                .orElseGet(() -> createInvitedUser(email, request.role(), request.workspaceId()));

        // Ensure workspace membership
        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", request.workspaceId()));
        if (workspace.getMembers().stream().noneMatch(m -> m.getId().equals(invited.getId()))) {
            workspace.getMembers().add(invited);
            workspaceRepository.save(workspace);
        }

        String otp = otpService.generateAndStore(email);
        emailService.sendInvite(email, otp);
    }

    private User createInvitedUser(String email, String role, Long workspaceId) {
        if (userRepository.existsByEmail(email)) {
            // email exists under a different workspace — still block to prevent enumeration
            throw new DuplicateEmailException(email);
        }
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRoles(new HashSet<>(Set.of(role)));
        user.setStatus(UserStatus.INVITED);
        user.setWorkspaceId(workspaceId);
        return userRepository.save(user);
    }

    // ── OTP Verification (INVITED → ACTIVE) ──────────────────────────────────

    /**
     * Verifies the OTP for an invited user.  On success the account becomes ACTIVE.
     * Throws {@link InvitationInvalidException} for any failure (prevents enumeration).
     */
    public User verifyInviteOtp(String email, String otp, String rawPassword) {
        String normalized = email.toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(InvitationInvalidException::new);

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new InvitationInvalidException();
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new InvitationInvalidException();
        }

        if (!otpService.validate(normalized, otp)) {
            throw new InvitationInvalidException();
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    // ── Disable / Enable ──────────────────────────────────────────────────────

    public void disableUser(Long targetUserId, User actingUser) {
        User target = requireUser(targetUserId);
        assertCanManage(actingUser, target);
        assertNotLastAdmin(target);

        target.setStatus(UserStatus.DISABLED);
        userRepository.save(target);
    }

    public void enableUser(Long targetUserId, User actingUser) {
        User target = requireUser(targetUserId);
        assertCanManage(actingUser, target);

        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);
    }

    // ── Change Role ───────────────────────────────────────────────────────────

    public void changeRole(Long targetUserId, String newRole, User actingUser) {
        if (!VALID_COMPANY_ROLES.contains(newRole)) {
            throw new BadRequestException("Invalid role: " + newRole);
        }
        User target = requireUser(targetUserId);
        assertCanManage(actingUser, target);

        // If demoting from COMPANY_ADMIN, ensure they're not the last one
        if (target.getRoles().contains(ROLE_COMPANY_ADMIN) && !newRole.equals(ROLE_COMPANY_ADMIN)) {
            assertNotLastAdmin(target);
        }

        target.setRoles(new HashSet<>(Set.of(newRole)));
        userRepository.save(target);
    }

    // ── Remove User ───────────────────────────────────────────────────────────

    public void removeUser(Long targetUserId, User actingUser) {
        User target = requireUser(targetUserId);
        assertCanManage(actingUser, target);
        assertNotLastAdmin(target);

        // Remove from all workspace member lists before deleting (FK constraint)
        workspaceRepository.findByMembers_Id(targetUserId).forEach(ws -> {
            ws.getMembers().removeIf(m -> m.getId().equals(targetUserId));
            workspaceRepository.save(ws);
        });

        userRepository.delete(target);
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<User> listWorkspaceUsers(Long workspaceId, User actingUser) {
        if (!isSuperAdmin(actingUser) && !isCompanyAdmin(actingUser)) {
            throw new UserOperationForbiddenException("Insufficient permissions");
        }
        if (!isSuperAdmin(actingUser) && !Objects.equals(workspaceId, actingUser.getWorkspaceId())) {
            throw new UserOperationForbiddenException("Cross-tenant access denied");
        }
        return userRepository.findByWorkspaceId(workspaceId);
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    private void assertCanManage(User acting, User target) {
        if (isSuperAdmin(acting)) return;
        if (!isCompanyAdmin(acting)) {
            throw new UserOperationForbiddenException("Insufficient permissions");
        }
        if (!Objects.equals(acting.getWorkspaceId(), target.getWorkspaceId())) {
            throw new UserOperationForbiddenException("Cross-tenant operation not allowed");
        }
    }

    private void assertCanInviteTo(User acting, Long workspaceId) {
        if (isSuperAdmin(acting)) return;
        if (!isCompanyAdmin(acting)) {
            throw new UserOperationForbiddenException("Insufficient permissions");
        }
        if (!Objects.equals(workspaceId, acting.getWorkspaceId())) {
            throw new UserOperationForbiddenException("Cross-tenant invite not allowed");
        }
    }

    private void assertNotLastAdmin(User target) {
        if (!target.getRoles().contains(ROLE_COMPANY_ADMIN)) return;
        if (target.getWorkspaceId() == null) return;

        long activeAdmins = userRepository.countByWorkspaceIdAndRoleAndStatus(
                target.getWorkspaceId(), ROLE_COMPANY_ADMIN, UserStatus.ACTIVE);
        if (activeAdmins <= 1) {
            throw new LastAdminException();
        }
    }

    public boolean isSuperAdmin(User user) {
        return user.getRoles().contains(ROLE_SUPER_ADMIN)
                || user.getRoles().contains(ROLE_ADMIN);
    }

    public boolean isCompanyAdmin(User user) {
        return user.getRoles().contains(ROLE_COMPANY_ADMIN) || isSuperAdmin(user);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
