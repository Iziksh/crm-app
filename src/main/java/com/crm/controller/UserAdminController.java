package com.crm.controller;

import com.crm.domain.entity.User;
import com.crm.dto.request.AdminInviteRequest;
import com.crm.dto.request.ChangeRoleRequest;
import com.crm.dto.request.VerifyInviteOtpRequest;
import com.crm.dto.response.AuthResponse;
import com.crm.dto.response.UserAdminResponse;
import com.crm.service.AdminUserManagementService;
import com.crm.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final AdminUserManagementService adminService;
    private final JwtService jwtService;

    public UserAdminController(AdminUserManagementService adminService, JwtService jwtService) {
        this.adminService = adminService;
        this.jwtService = jwtService;
    }

    /** Invite a user by email (sends OTP). */
    @PostMapping("/invite")
    public ResponseEntity<Map<String, String>> invite(
            @Valid @RequestBody AdminInviteRequest request,
            @AuthenticationPrincipal User actingUser) {
        adminService.inviteUser(request, actingUser);
        return ResponseEntity.accepted().body(Map.of("message", "Invitation sent"));
    }

    /**
     * Verify invite OTP (public endpoint — not yet authenticated).
     * Returns a JWT so the client is immediately signed in after activation.
     */
    @PostMapping("/verify-invite")
    public ResponseEntity<AuthResponse> verifyInvite(
            @Valid @RequestBody VerifyInviteOtpRequest request) {
        User activated = adminService.verifyInviteOtp(request.email(), request.otp(), request.password());
        String token = jwtService.generateToken(activated);
        return ResponseEntity.ok(new AuthResponse(token, activated.getUsername(), activated.getEmail()));
    }

    /** Disable a user account. */
    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable Long id,
                                        @AuthenticationPrincipal User actingUser) {
        adminService.disableUser(id, actingUser);
        return ResponseEntity.noContent().build();
    }

    /** Re-enable a previously disabled account. */
    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable Long id,
                                       @AuthenticationPrincipal User actingUser) {
        adminService.enableUser(id, actingUser);
        return ResponseEntity.noContent().build();
    }

    /** Change a user's role. */
    @PutMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long id,
                                           @Valid @RequestBody ChangeRoleRequest request,
                                           @AuthenticationPrincipal User actingUser) {
        adminService.changeRole(id, request.role(), actingUser);
        return ResponseEntity.noContent().build();
    }

    /** Permanently remove a user from the company. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id,
                                       @AuthenticationPrincipal User actingUser) {
        adminService.removeUser(id, actingUser);
        return ResponseEntity.noContent().build();
    }

    /** List all users in a workspace (company). */
    @GetMapping
    public ResponseEntity<List<UserAdminResponse>> listUsers(
            @RequestParam Long workspaceId,
            @AuthenticationPrincipal User actingUser) {
        List<UserAdminResponse> users = adminService.listWorkspaceUsers(workspaceId, actingUser)
                .stream().map(UserAdminResponse::from).toList();
        return ResponseEntity.ok(users);
    }
}
