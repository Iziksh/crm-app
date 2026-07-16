package com.crm.controller;

import com.crm.domain.entity.User;
import com.crm.dto.request.AcceptInvitationRequest;
import com.crm.dto.request.InviteRequest;
import com.crm.dto.response.AuthResponse;
import com.crm.repository.UserRepository;
import com.crm.service.InvitationService;
import com.crm.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public InvitationController(InvitationService invitationService, UserRepository userRepository,
                                 JwtService jwtService) {
        this.invitationService = invitationService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> invite(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        invitationService.createInvitation(request, principal.getUsername());
        return ResponseEntity.accepted().body(Map.of("message", "Invitation sent"));
    }

    /** Public: creates the account from a token-based invitation link and signs the user in. */
    @PostMapping("/accept")
    public ResponseEntity<AuthResponse> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitation(request.token(), request.username(), request.password());
        User user = userRepository.findByUsername(request.username()).orElseThrow();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail()));
    }
}