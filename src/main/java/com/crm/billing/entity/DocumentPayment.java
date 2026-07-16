package com.crm.billing.entity;

import com.crm.billing.enums.PaymentMethod;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A payment recorded against an issued TaxDocument (סוג תשלום + סכום section in the editor). */
@Entity
@Table(name = "document_payments")
public class DocumentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_document_id", nullable = false)
    private TaxDocument taxDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "received_at")
    private LocalDate receivedAt = LocalDate.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TaxDocument getTaxDocument() { return taxDocument; }
    public void setTaxDocument(TaxDocument taxDocument) { this.taxDocument = taxDocument; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDate receivedAt) { this.receivedAt = receivedAt; }
}
