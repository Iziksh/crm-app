package com.crm.billing.entity;

import com.crm.billing.enums.DocumentOwnerType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** An unauthenticated, expiring public download link for a single document (PDF), used by WhatsApp
 * sharing since wa.me/api.whatsapp.com links can only pre-fill text, never attach a file — the
 * recipient (not a CRM user) taps this link instead. Same token-hashing convention as
 * {@code Invitation}/{@code TrustedDevice}: only the SHA-256 hash is ever persisted, never the raw
 * token, and multi-use (not burned on first download) until it expires. */
@Entity
@Table(name = "document_share_links")
public class DocumentShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public DocumentOwnerType getOwnerType() { return ownerType; }
    public void setOwnerType(DocumentOwnerType ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
