package com.crm.billing.entity;

import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** דרישת תשלום — tax-neutral, editable while {@code OPEN}. Locked once {@code CONVERTED}. */
@Entity
@Table(name = "payment_requests")
@EntityListeners(AuditingEntityListener.class)
public class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Sequential per (workspace, documentYear); see {@link PaymentRequestSequence}. Null while unissued drafts aren't supported — assigned at creation, since payment requests have no separate draft/issue split. */
    @Column(name = "number")
    private String number;

    @Column(name = "document_year")
    private Integer documentYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentRequestStatus status = PaymentRequestStatus.OPEN;

    @Column(nullable = false, length = 3)
    private String currency = "ILS";

    @Column(name = "document_date")
    private LocalDate documentDate = LocalDate.now();

    @Column(name = "free_text", columnDefinition = "TEXT")
    private String freeText;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "converted_document_id")
    private Long convertedDocumentId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Integer getDocumentYear() { return documentYear; }
    public void setDocumentYear(Integer documentYear) { this.documentYear = documentYear; }
    public PaymentRequestStatus getStatus() { return status; }
    public void setStatus(PaymentRequestStatus status) { this.status = status; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
    public String getFreeText() { return freeText; }
    public void setFreeText(String freeText) { this.freeText = freeText; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Long getConvertedDocumentId() { return convertedDocumentId; }
    public void setConvertedDocumentId(Long convertedDocumentId) { this.convertedDocumentId = convertedDocumentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
