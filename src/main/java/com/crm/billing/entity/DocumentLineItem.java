package com.crm.billing.entity;

import com.crm.billing.enums.DocumentOwnerType;
import jakarta.persistence.*;

import java.math.BigDecimal;

/** Line item shared by both PaymentRequest and TaxDocument, via a polymorphic owner reference
 * (same "one table, multiple owning types" pattern as {@code Attachment.entityType}/{@code entityId}
 * from Phase 16) rather than two thin entities over a shared embeddable. */
@Entity
@Table(name = "document_line_items")
public class DocumentLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** מוצר/שירות */
    @Column(name = "product_or_service", nullable = false)
    private String productOrService;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 15, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @PrePersist
    @PreUpdate
    void calcLineTotal() {
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ZERO;
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        lineTotal = qty.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentOwnerType getOwnerType() { return ownerType; }
    public void setOwnerType(DocumentOwnerType ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getProductOrService() { return productOrService; }
    public void setProductOrService(String productOrService) { this.productOrService = productOrService; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
