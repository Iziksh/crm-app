package com.crm.service;

import com.crm.domain.entity.Invitation;
import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.UserStatus;
import com.crm.dto.request.InviteRequest;
import com.crm.exception.BadRequestException;
import com.crm.exception.InvitationInvalidException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.InvitationRepository;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class InvitationService {

    private static final int EXPIRY_HOURS = 72;
    private static final Set<String> VALID_ROLES =
            Set.of("ROLE_USER", "ROLE_ADMIN", "ROLE_SALES", "ROLE_SUPPORT");

    private final InvitationRepository invitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String baseUrl;

    public InvitationService(InvitationRepository invitationRepository,
                             WorkspaceRepository workspaceRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             EmailService emailService,
                             @Value("${app.base-url:http://localhost:9080}") String baseUrl) {
        this.invitationRepository = invitationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a single-use invitation and sends it by email.
     * Returns the raw token (not persisted) for testing; the caller should not store it.
     */
    public String createInvitation(InviteRequest request, String adminUsername) {
        if (!VALID_ROLES.contains(request.role())) {
            throw new BadRequestException("Invalid role: " + request.role());
        }
        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", request.workspaceId()));

        String rawToken = UUID.randomUUID().toString();

        Invitation invitation = new Invitation();
        invitation.setTokenHash(sha256(rawToken));
        invitation.setEmail(request.email().toLowerCase());
        invitation.setRole(request.role());
        invitation.setWorkspace(workspace);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(EXPIRY_HOURS));
        invitationRepository.save(invitation);

        String link = baseUrl + "/accept-invite/" + rawToken;
        //emailService.sendInvitation(request.email(), link);

        return rawToken;
    }

    /**
     * Validates the raw invitation token and creates the user account.
     * All failure modes (invalid, expired, already used, email conflict, username conflict)
     * throw the same InvitationInvalidException to prevent enumeration.
     */
    public void acceptInvitation(String rawToken, String username, String password) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvitationInvalidException();
        }

        Invitation invitation = invitationRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(InvitationInvalidException::new);

        if (invitation.isExpired() || invitation.isAccepted()) {
            throw new InvitationInvalidException();
        }

        // Generic error for both username-taken and email-taken — no enumeration
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(invitation.getEmail())) {
            throw new InvitationInvalidException();
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(invitation.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new java.util.HashSet<>(Set.of(invitation.getRole())));
        user.setStatus(UserStatus.ACTIVE);
        user.setWorkspaceId(invitation.getWorkspace().getId());
        userRepository.save(user);

        // Add user to the invited workspace — workspace must always be set
        Workspace workspace = workspaceRepository.findById(invitation.getWorkspace().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", invitation.getWorkspace().getId()));
        workspace.getMembers().add(user);
        workspaceRepository.save(workspace);

        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpired() {
        invitationRepository.deleteByExpiresAtBeforeAndAcceptedAtIsNull(LocalDateTime.now());
    }

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
