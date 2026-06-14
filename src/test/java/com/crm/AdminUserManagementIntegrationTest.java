package com.crm;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.UserStatus;
import com.crm.dto.request.AdminInviteRequest;
import com.crm.exception.InvitationInvalidException;
import com.crm.exception.LastAdminException;
import com.crm.exception.UserOperationForbiddenException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.crm.service.AdminUserManagementService;
import com.crm.service.OtpService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserManagementIntegrationTest {

    @Autowired AdminUserManagementService adminService;
    @Autowired UserRepository             userRepository;
    @Autowired WorkspaceRepository        workspaceRepository;
    @Autowired PasswordEncoder            passwordEncoder;
    @Autowired OtpService                 otpService;

    @MockBean JavaMailSender mailSender;

    private Workspace wsA;
    private Workspace wsB;
    private User      adminA;

    @BeforeEach
    void setUp() {
        MimeMessage fakeMime = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(fakeMime);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        wsA = workspaceRepository.save(makeWs("Company A"));
        wsB = workspaceRepository.save(makeWs("Company B"));

        adminA = userRepository.save(makeUser("adminA@a.com", "ROLE_COMPANY_ADMIN", wsA.getId()));
    }

    // ── INVITED → ACTIVE via OTP ──────────────────────────────────────────

    @Test
    void inviteAndActivate_fullFlow() {
        AdminInviteRequest req = new AdminInviteRequest("new@a.com", "ROLE_USER", wsA.getId());
        adminService.inviteUser(req, adminA);

        User invited = userRepository.findByEmail("new@a.com").orElseThrow();
        assertThat(invited.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(invited.isEnabled()).isFalse();
        assertThat(invited.getWorkspaceId()).isEqualTo(wsA.getId());

        // Simulate correct OTP
        String otp = otpService.generateAndStore("new@a.com");
        User activated = adminService.verifyInviteOtp("new@a.com", otp);

        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(activated.isEnabled()).isTrue();
    }

    @Test
    void disabledUser_cannotReceiveOtp() {
        User disabled = userRepository.save(makeUser("dis@a.com", "ROLE_USER", wsA.getId()));
        disabled.setStatus(UserStatus.DISABLED);
        userRepository.save(disabled);

        AdminInviteRequest req = new AdminInviteRequest("dis@a.com", "ROLE_USER", wsA.getId());
        assertThatThrownBy(() -> adminService.inviteUser(req, adminA))
                .isInstanceOf(UserOperationForbiddenException.class);
    }

    // ── Last admin protection ─────────────────────────────────────────────

    @Test
    void cannotDisableLastAdmin() {
        // adminA is the only COMPANY_ADMIN in wsA
        assertThatThrownBy(() -> adminService.disableUser(adminA.getId(), adminA))
                .isInstanceOf(LastAdminException.class);

        User unchanged = userRepository.findById(adminA.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void canDisableAdminWhenAnotherExists() {
        User adminA2 = userRepository.save(makeUser("admin2@a.com", "ROLE_COMPANY_ADMIN", wsA.getId()));

        adminService.disableUser(adminA2.getId(), adminA);

        User result = userRepository.findById(adminA2.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void cannotDemoteLastAdmin() {
        assertThatThrownBy(() -> adminService.changeRole(adminA.getId(), "ROLE_USER", adminA))
                .isInstanceOf(LastAdminException.class);
    }

    @Test
    void cannotRemoveLastAdmin() {
        assertThatThrownBy(() -> adminService.removeUser(adminA.getId(), adminA))
                .isInstanceOf(LastAdminException.class);

        assertThat(userRepository.existsById(adminA.getId())).isTrue();
    }

    // ── Cross-tenant isolation ────────────────────────────────────────────

    @Test
    void adminCannotActOnOtherTenantUser() {
        User userInB = userRepository.save(makeUser("b@b.com", "ROLE_USER", wsB.getId()));

        assertThatThrownBy(() -> adminService.disableUser(userInB.getId(), adminA))
                .isInstanceOf(UserOperationForbiddenException.class);
    }

    @Test
    void adminCannotInviteToOtherTenant() {
        AdminInviteRequest req = new AdminInviteRequest("x@b.com", "ROLE_USER", wsB.getId());

        assertThatThrownBy(() -> adminService.inviteUser(req, adminA))
                .isInstanceOf(UserOperationForbiddenException.class);
    }

    @Test
    void superAdminCanActCrossTenant() {
        User super_ = userRepository.save(makeUser("super@crm.com", "ROLE_ADMIN", null));
        User userInB = userRepository.save(makeUser("b@b.com", "ROLE_USER", wsB.getId()));

        // Should not throw
        adminService.disableUser(userInB.getId(), super_);

        assertThat(userRepository.findById(userInB.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.DISABLED);
    }

    // ── Invite resend for INVITED user ────────────────────────────────────

    @Test
    void reinviteAlreadyInvitedUserResendOtp() {
        AdminInviteRequest req = new AdminInviteRequest("inv@a.com", "ROLE_USER", wsA.getId());
        adminService.inviteUser(req, adminA); // first invite

        // Second invite should not fail — just resends OTP
        adminService.inviteUser(req, adminA);

        // Still only one user with that email
        assertThat(userRepository.findByEmail("inv@a.com")).isPresent();
    }

    @Test
    void inviteWithBadOtpFails() {
        AdminInviteRequest req = new AdminInviteRequest("bad@a.com", "ROLE_USER", wsA.getId());
        adminService.inviteUser(req, adminA);

        assertThatThrownBy(() -> adminService.verifyInviteOtp("bad@a.com", "000000"))
                .isInstanceOf(InvitationInvalidException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Workspace makeWs(String name) {
        Workspace ws = new Workspace();
        ws.setName(name);
        return ws;
    }

    private User makeUser(String email, String role, Long wsId) {
        User u = new User();
        u.setUsername(email);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("password123!"));
        Set<String> roles = new java.util.HashSet<>();
        roles.add(role);
        u.setRoles(roles);
        u.setStatus(UserStatus.ACTIVE);
        u.setWorkspaceId(wsId);
        return u;
    }
}
