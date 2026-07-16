package com.crm.billing.service;

import com.crm.billing.entity.DocumentShareLink;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.repository.DocumentShareLinkRepository;
import com.crm.exception.ResourceNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/** Creates and resolves the expiring public download tokens used by WhatsApp sharing (see
 * {@link com.crm.billing.entity.DocumentShareLink}). Same raw-token/SHA-256-hash convention as
 * {@code InvitationService} — the raw token is only ever returned once, at creation time, and never
 * stored. */
@Service
@Transactional
public class DocumentShareLinkService {

    private static final int EXPIRY_DAYS = 7;

    private final DocumentShareLinkRepository repository;

    public DocumentShareLinkService(DocumentShareLinkRepository repository) {
        this.repository = repository;
    }

    public String createLink(DocumentOwnerType ownerType, Long ownerId) {
        String rawToken = UUID.randomUUID().toString();
        DocumentShareLink link = new DocumentShareLink();
        link.setTokenHash(sha256(rawToken));
        link.setOwnerType(ownerType);
        link.setOwnerId(ownerId);
        link.setExpiresAt(LocalDateTime.now().plusDays(EXPIRY_DAYS));
        repository.save(link);
        return rawToken;
    }

    /** Returns the (ownerType, ownerId) a valid, unexpired token points to. Throws
     * {@link ResourceNotFoundException} (mapped to a plain 404) for anything invalid/expired —
     * never distinguishes the reason, so an attacker can't tell "wrong token" from "expired token"
     * from "never existed". */
    @Transactional(readOnly = true)
    public DocumentShareLink resolve(String rawToken) {
        DocumentShareLink link = repository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("DocumentShareLink", "token", "***"));
        if (link.isExpired()) {
            throw new ResourceNotFoundException("DocumentShareLink", "token", "***");
        }
        return link;
    }

    @Scheduled(cron = "0 30 4 * * *")
    public void cleanupExpired() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
