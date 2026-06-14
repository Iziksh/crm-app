package com.crm.controller;

import com.crm.dto.request.InviteRequest;
import com.crm.service.InvitationService;
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

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> invite(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        invitationService.createInvitation(request, principal.getUsername());
        return ResponseEntity.accepted().body(Map.of("message", "Invitation sent"));
    }
}