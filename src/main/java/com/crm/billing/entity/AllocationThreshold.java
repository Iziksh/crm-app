package com.crm.billing.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Effective-dated מספר הקצאה threshold (net-before-VAT). Global across tenants — driven by Israeli
 * tax law, not per-workspace configuration. The applicable row for a given document date is the one
 * with the latest {@code effectiveDate <= documentDate}. */
@Entity
@Table(name = "allocation_thresholds")
public class AllocationThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "threshold_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal thresholdAmount;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
}
