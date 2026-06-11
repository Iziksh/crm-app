package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.UserStatus;
import com.crm.dto.request.AdminInviteRequest;
import com.crm.exception.BadRequestException;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.LastAdminException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AdminUserManagementService {

    private static final String COMPANY_ADMIN_ROLE = "ROLE_COMPANY_ADMIN";

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    public AdminUserManagementService(UserRepository userRepository,
                                      WorkspaceRepository workspaceRepository,
                                      PasswordEncoder passwordEncoder,
                                      OtpService otpService,
                                      EmailService emailService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    public boolean isSuperAdmin(User user) {
        return user.getRoles() != null && user.getRoles().contains("ROLE_SUPER_ADMIN");
    }

    @Transactional(readOnly = true)
    public List<User> listWorkspaceUsers(Long workspaceId, User actingUser) {
        assertCanManageWorkspace(workspaceId, actingUser);
        return userRepository.findByWorkspaceId(workspaceId);
    }

    public void inviteUser(AdminInviteRequest request, User actingUser) {
        Long workspaceId = request.workspaceId();
        if (workspaceId == null) {
            throw new BadRequestException("Workspace is required");
        }
        assertCanManageWorkspace(workspaceId, actingUser);

        String email = request.email().trim().toLowerCase();
        String role = request.role() != null ? request.role() : "ROLE_USER";

        User user = userRepository.findByEmailAndWorkspaceId(email, workspaceId).orElse(null);
        if (user == null && userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(generateUsername(email));
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRoles(Set.of(role));
            user.setWorkspaceId(workspaceId);
            user.setStatus(UserStatus.INVITED);
            user = userRepository.save(user);
            addToWorkspace(workspaceId, user);
        } else if (user.getStatus() != UserStatus.INVITED) {
            throw new BadRequestException("User already exists and is not pending invitation");
        } else {
            user.setRoles(Set.of(role));
            userRepository.save(user);
        }

        String otp = otpService.generateAndStore(email);
        emailService.sendInvite(email, otp);
    }

    public void disableUser(Long userId, User actingUser) throws LastAdminException {
        User target = getManagedUser(userId, actingUser);
        assertNotLastCompanyAdmin(target, "disable");
        target.setStatus(UserStatus.DISABLED);
        userRepository.save(target);
    }

    public void enableUser(Long userId, User actingUser) {
        User target = getManagedUser(userId, actingUser);
        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);
    }

    public void changeRole(Long userId, String role, User actingUser) throws LastAdminException {
        User target = getManagedUser(userId, actingUser);
        if (target.getRoles().contains(COMPANY_ADMIN_ROLE) && !COMPANY_ADMIN_ROLE.equals(role)) {
            assertNotLastCompanyAdmin(target, "demote");
        }
        target.setRoles(Set.of(role));
        userRepository.save(target);
    }

    public void removeUser(Long userId, User actingUser) throws LastAdminException {
        User target = getManagedUser(userId, actingUser);
        assertNotLastCompanyAdmin(target, "remove");
        userRepository.delete(target);
    }

    private User getManagedUser(Long userId, User actingUser) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (target.getUsername().equals(actingUser.getUsername())) {
            throw new BadRequestException("Cannot modify your own account through this action");
        }
        if (isSuperAdmin(actingUser)) {
            return target;
        }
        if (target.getWorkspaceId() == null || !target.getWorkspaceId().equals(actingUser.getWorkspaceId())) {
            throw new BadRequestException("User is not in your workspace");
        }
        assertCanManageWorkspace(target.getWorkspaceId(), actingUser);
        return target;
    }

    private void assertCanManageWorkspace(Long workspaceId, User actingUser) {
        if (isSuperAdmin(actingUser)) {
            return;
        }
        if (actingUser.getRoles() == null || !actingUser.getRoles().contains(COMPANY_ADMIN_ROLE)) {
            throw new BadRequestException("Insufficient permissions");
        }
        if (actingUser.getWorkspaceId() == null || !actingUser.getWorkspaceId().equals(workspaceId)) {
            throw new BadRequestException("Cannot manage users outside your workspace");
        }
    }

    private void assertNotLastCompanyAdmin(User target, String action) throws LastAdminException {
        if (target.getWorkspaceId() == null || !target.getRoles().contains(COMPANY_ADMIN_ROLE)) {
            return;
        }
        UserStatus status = target.getStatus() != null ? target.getStatus()
                : (target.isEnabled() ? UserStatus.ACTIVE : UserStatus.DISABLED);
        if (status != UserStatus.ACTIVE) {
            return;
        }
        long activeAdmins = userRepository.countByWorkspaceIdAndRoleAndStatus(
                target.getWorkspaceId(), COMPANY_ADMIN_ROLE, UserStatus.ACTIVE);
        if (activeAdmins <= 1) {
            throw new LastAdminException("Cannot " + action + " the last company admin");
        }
    }

    private void addToWorkspace(Long workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        if (workspace.getMembers().stream().noneMatch(m -> m.getId().equals(user.getId()))) {
            workspace.getMembers().add(user);
            workspaceRepository.save(workspace);
        }
    }

    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
