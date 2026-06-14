package com.crm;

import com.crm.domain.entity.User;
import com.crm.domain.enums.UserStatus;
import com.crm.dto.request.AdminInviteRequest;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.InvitationInvalidException;
import com.crm.exception.LastAdminException;
import com.crm.exception.UserOperationForbiddenException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.crm.service.AdminUserManagementService;
import com.crm.service.EmailService;
import com.crm.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

    @Mock UserRepository      userRepository;
    @Mock WorkspaceRepository workspaceRepository;
    @Mock PasswordEncoder     passwordEncoder;
    @Mock OtpService          otpService;
    @Mock EmailService        emailService;

    AdminUserManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(
                userRepository, workspaceRepository, passwordEncoder, otpService, emailService);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User adminUser(Long id, Long wsId) {
        User u = new User();
        u.setId(id);
        u.setUsername("admin" + id);
        u.setEmail("admin" + id + "@co.com");
        u.setRoles(mutableRoles("ROLE_COMPANY_ADMIN"));
        u.setStatus(UserStatus.ACTIVE);
        u.setWorkspaceId(wsId);
        return u;
    }

    private User regularUser(Long id, Long wsId) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setEmail("user" + id + "@co.com");
        u.setRoles(mutableRoles("ROLE_USER"));
        u.setStatus(UserStatus.ACTIVE);
        u.setWorkspaceId(wsId);
        return u;
    }

    private User superAdmin(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("super" + id);
        u.setEmail("super" + id + "@crm.com");
        u.setRoles(mutableRoles("ROLE_ADMIN"));
        u.setStatus(UserStatus.ACTIVE);
        u.setWorkspaceId(null);
        return u;
    }

    private static Set<String> mutableRoles(String role) {
        Set<String> s = new java.util.HashSet<>();
        s.add(role);
        return s;
    }

    // ── INVITED → ACTIVE transition ───────────────────────────────────────

    @Test
    void verifyInviteOtp_activatesInvitedUser() {
        User invited = new User();
        invited.setEmail("new@co.com");
        invited.setStatus(UserStatus.INVITED);
        when(userRepository.findByEmail("new@co.com")).thenReturn(Optional.of(invited));
        when(otpService.validate("new@co.com", "123456")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.verifyInviteOtp("new@co.com", "123456", "password123!");

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void verifyInviteOtp_invalidOtpThrows() {
        User invited = new User();
        invited.setEmail("new@co.com");
        invited.setStatus(UserStatus.INVITED);
        when(userRepository.findByEmail("new@co.com")).thenReturn(Optional.of(invited));
        when(otpService.validate(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyInviteOtp("new@co.com", "000000", "password123!"))
                .isInstanceOf(InvitationInvalidException.class);
    }

    @Test
    void verifyInviteOtp_disabledUserThrows() {
        User disabled = new User();
        disabled.setEmail("dis@co.com");
        disabled.setStatus(UserStatus.DISABLED);
        when(userRepository.findByEmail("dis@co.com")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.verifyInviteOtp("dis@co.com", "123456", "password123!"))
                .isInstanceOf(InvitationInvalidException.class);
        verifyNoInteractions(otpService);
    }

    @Test
    void verifyInviteOtp_activeUserThrows() {
        User active = new User();
        active.setEmail("active@co.com");
        active.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("active@co.com")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.verifyInviteOtp("active@co.com", "123456", "password123!"))
                .isInstanceOf(InvitationInvalidException.class);
    }

    // ── Last admin protection ─────────────────────────────────────────────

    @Test
    void disableUser_lastAdminThrows() {
        User actor = adminUser(1L, 10L);
        User target = adminUser(2L, 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByWorkspaceIdAndRoleAndStatus(10L, "ROLE_COMPANY_ADMIN", UserStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.disableUser(2L, actor))
                .isInstanceOf(LastAdminException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void disableUser_notLastAdminSucceeds() {
        User actor = adminUser(1L, 10L);
        User target = adminUser(2L, 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByWorkspaceIdAndRoleAndStatus(10L, "ROLE_COMPANY_ADMIN", UserStatus.ACTIVE))
                .thenReturn(2L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.disableUser(2L, actor);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.DISABLED));
    }

    @Test
    void changeRole_demotingLastAdminThrows() {
        User actor = adminUser(1L, 10L);
        User target = adminUser(2L, 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByWorkspaceIdAndRoleAndStatus(10L, "ROLE_COMPANY_ADMIN", UserStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(2L, "ROLE_USER", actor))
                .isInstanceOf(LastAdminException.class);
    }

    @Test
    void removeUser_lastAdminThrows() {
        User actor = adminUser(1L, 10L);
        User target = adminUser(2L, 10L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByWorkspaceIdAndRoleAndStatus(10L, "ROLE_COMPANY_ADMIN", UserStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.removeUser(2L, actor))
                .isInstanceOf(LastAdminException.class);
        verify(userRepository, never()).delete(any());
    }

    // ── Cross-tenant enforcement ──────────────────────────────────────────

    @Test
    void disableUser_crossTenantForbidden() {
        User actor  = adminUser(1L, 10L);
        User target = regularUser(3L, 20L); // different workspace!
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.disableUser(3L, actor))
                .isInstanceOf(UserOperationForbiddenException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void changeRole_crossTenantForbidden() {
        User actor  = adminUser(1L, 10L);
        User target = regularUser(3L, 20L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.changeRole(3L, "ROLE_USER", actor))
                .isInstanceOf(UserOperationForbiddenException.class);
    }

    @Test
    void superAdmin_canActCrossTenant() {
        User actor  = superAdmin(99L);
        User target = regularUser(3L, 20L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw
        service.disableUser(3L, actor);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.DISABLED));
    }

    // ── Invite guards ─────────────────────────────────────────────────────

    @Test
    void invite_activeUserThrowsDuplicate() {
        User actor   = adminUser(1L, 10L);
        User existing = regularUser(5L, 10L);
        existing.setEmail("dup@co.com");
        existing.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmailAndWorkspaceId("dup@co.com", 10L))
                .thenReturn(Optional.of(existing));

        AdminInviteRequest req = new AdminInviteRequest("dup@co.com", "ROLE_USER", 10L);
        assertThatThrownBy(() -> service.inviteUser(req, actor))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void invite_disabledUserForbidden() {
        User actor   = adminUser(1L, 10L);
        User existing = regularUser(5L, 10L);
        existing.setEmail("dis@co.com");
        existing.setStatus(UserStatus.DISABLED);

        when(userRepository.findByEmailAndWorkspaceId("dis@co.com", 10L))
                .thenReturn(Optional.of(existing));

        AdminInviteRequest req = new AdminInviteRequest("dis@co.com", "ROLE_USER", 10L);
        assertThatThrownBy(() -> service.inviteUser(req, actor))
                .isInstanceOf(UserOperationForbiddenException.class);
    }

    @Test
    void invite_crossTenantForbidden() {
        User actor = adminUser(1L, 10L);
        AdminInviteRequest req = new AdminInviteRequest("new@co.com", "ROLE_USER", 99L);

        assertThatThrownBy(() -> service.inviteUser(req, actor))
                .isInstanceOf(UserOperationForbiddenException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void invite_nonAdminForbidden() {
        User actor = regularUser(1L, 10L);
        AdminInviteRequest req = new AdminInviteRequest("new@co.com", "ROLE_USER", 10L);

        assertThatThrownBy(() -> service.inviteUser(req, actor))
                .isInstanceOf(UserOperationForbiddenException.class)
                .hasMessageContaining("Insufficient");
    }
}
