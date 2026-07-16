package com.crm.billing.entity;

import jakarta.persistence.*;

/** One row per (workspace, year). {@code lastNumber} is incremented under a row lock by
 * {@code PaymentRequestNumberingService} to keep numbering monotonic/collision-free
 * (gaps are legally acceptable for payment requests, unlike tax documents). */
@Entity
@Table(name = "payment_request_sequences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "year"}))
public class PaymentRequestSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "last_number", nullable = false)
    private long lastNumber = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public long getLastNumber() { return lastNumber; }
    public void setLastNumber(long lastNumber) { this.lastNumber = lastNumber; }
}
