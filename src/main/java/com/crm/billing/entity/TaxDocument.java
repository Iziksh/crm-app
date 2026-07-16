package com.crm.billing.entity;

import com.crm.billing.enums.AllocationStatus;
import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tax-compliant document (חשבונית מס/קבלה, קבלה, חשבון עסקה, הצעת מחיר, חשבונית זיכוי). Immutable once
 * {@link DocumentStatus#ISSUED} — corrections happen via a {@link DocumentType#CREDIT_INVOICE}
 * referencing {@link #originalDocumentId}, never by editing this row. */
@Entity
@Table(name = "tax_documents")
@EntityListeners(AuditingEntityListener.class)
public class TaxDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    /** Sequential per (workspace, documentType, documentYear), gap-free; assigned only at issue time
     * by {@code TaxDocumentNumberingService} — null while DRAFT. */
    @Column(name = "number")
    private String number;

    @Column(name = "document_year")
    private Integer documentYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Column(nullable = false, length = 3)
    private String currency = "ILS";

    @Column(name = "document_date")
    private LocalDate documentDate = LocalDate.now();

    @Column(name = "free_text", columnDefinition = "TEXT")
    private String freeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_type", nullable = false, length = 20)
    private VatType vatType = VatType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false, length = 20)
    private RoundingMode roundingMode = RoundingMode.NONE;

    /** Net total after line items + discount, before VAT. */
    @Column(name = "net_total", precision = 15, scale = 2)
    private BigDecimal netTotal = BigDecimal.ZERO;

    /** VAT rate snapshot at calculation time (e.g. 0.18) — stored so historical totals stay
     * reproducible even if {@code tax.vat.standard-rate} changes later. */
    @Column(name = "vat_rate", precision = 5, scale = 4)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    /** Gross total before rounding is applied. */
    @Column(name = "pre_round_total", precision = 15, scale = 2)
    private BigDecimal preRoundTotal = BigDecimal.ZERO;

    /** preRoundTotal - grossTotal (signed), kept so totals remain auditable/reproducible. */
    @Column(name = "rounding_delta", precision = 15, scale = 2)
    private BigDecimal roundingDelta = BigDecimal.ZERO;

    /** Final total shown on the document, after rounding. */
    @Column(name = "gross_total", precision = 15, scale = 2)
    private BigDecimal grossTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false, length = 20)
    private AllocationStatus allocationStatus = AllocationStatus.NOT_REQUIRED;

    @Column(name = "allocation_number")
    private String allocationNumber;

    /** For CREDIT_INVOICE: the TaxDocument.id being corrected. */
    @Column(name = "original_document_id")
    private Long originalDocumentId;

    /** Set when this document was produced by converting a PaymentRequest. */
    @Column(name = "source_payment_request_id")
    private Long sourcePaymentRequestId;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

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
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Integer getDocumentYear() { return documentYear; }
    public void setDocumentYear(Integer documentYear) { this.documentYear = documentYear; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
    public String getFreeText() { return freeText; }
    public void setFreeText(String freeText) { this.freeText = freeText; }
    public VatType getVatType() { return vatType; }
    public void setVatType(VatType vatType) { this.vatType = vatType; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public RoundingMode getRoundingMode() { return roundingMode; }
    public void setRoundingMode(RoundingMode roundingMode) { this.roundingMode = roundingMode; }
    public BigDecimal getNetTotal() { return netTotal; }
    public void setNetTotal(BigDecimal netTotal) { this.netTotal = netTotal; }
    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }
    public BigDecimal getPreRoundTotal() { return preRoundTotal; }
    public void setPreRoundTotal(BigDecimal preRoundTotal) { this.preRoundTotal = preRoundTotal; }
    public BigDecimal getRoundingDelta() { return roundingDelta; }
    public void setRoundingDelta(BigDecimal roundingDelta) { this.roundingDelta = roundingDelta; }
    public BigDecimal getGrossTotal() { return grossTotal; }
    public void setGrossTotal(BigDecimal grossTotal) { this.grossTotal = grossTotal; }
    public AllocationStatus getAllocationStatus() { return allocationStatus; }
    public void setAllocationStatus(AllocationStatus allocationStatus) { this.allocationStatus = allocationStatus; }
    public String getAllocationNumber() { return allocationNumber; }
    public void setAllocationNumber(String allocationNumber) { this.allocationNumber = allocationNumber; }
    public Long getOriginalDocumentId() { return originalDocumentId; }
    public void setOriginalDocumentId(Long originalDocumentId) { this.originalDocumentId = originalDocumentId; }
    public Long getSourcePaymentRequestId() { return sourcePaymentRequestId; }
    public void setSourcePaymentRequestId(Long sourcePaymentRequestId) { this.sourcePaymentRequestId = sourcePaymentRequestId; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
