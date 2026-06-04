package com.crm.service;

import com.crm.domain.entity.TrustedDevice;
import com.crm.repository.TrustedDeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceTrustService {

    private final TrustedDeviceRepository repo;
    private final int trustDays;
    private final String cookieName;

    public DeviceTrustService(TrustedDeviceRepository repo,
                              @Value("${device-trust.days:14}") int trustDays,
                              @Value("${device-trust.cookie-name:DEVICE_TRUST}") String cookieName) {
        this.repo = repo;
        this.trustDays = trustDays;
        this.cookieName = cookieName;
    }

    public String getCookieName() { return cookieName; }
    public int getTrustDays() { return trustDays; }

    /** Creates a trust record, returns the raw token to store in a cookie. */
    public String createTrustToken(String userEmail) {
        String rawToken = UUID.randomUUID().toString();
        TrustedDevice device = new TrustedDevice();
        device.setUserEmail(userEmail.toLowerCase());
        device.setTokenHash(sha256(rawToken));
        device.setExpiresAt(LocalDateTime.now().plusDays(trustDays));
        repo.save(device);
        return rawToken;
    }

    /** Returns true if the raw token from the cookie is valid for this user. */
    public boolean isTokenValid(String rawToken, String userEmail) {
        if (rawToken == null || userEmail == null) return false;
        Optional<TrustedDevice> device = repo.findByTokenHash(sha256(rawToken));
        return device.isPresent()
                && device.get().getUserEmail().equalsIgnoreCase(userEmail)
                && device.get().getExpiresAt().isAfter(LocalDateTime.now());
    }

    /** Revokes all trusted devices for a user (e.g. on password change). */
    public void revokeAll(String userEmail) {
        repo.deleteByUserEmail(userEmail.toLowerCase());
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpired() {
        repo.deleteExpired(LocalDateTime.now());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
