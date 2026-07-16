package com.crm.billing.entity;

import com.crm.billing.enums.DocumentType;
import jakarta.persistence.*;

/** One row per (workspace, documentType, year). {@code lastNumber} is incremented under a row lock
 * by {@code TaxDocumentNumberingService}, only at issue time, to guarantee gap-free/collision-free
 * sequential numbering per Israeli tax-invoice requirements. */
@Entity
@Table(name = "tax_document_sequences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "document_type", "year"}))
public class TaxDocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "last_number", nullable = false)
    private long lastNumber = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public long getLastNumber() { return lastNumber; }
    public void setLastNumber(long lastNumber) { this.lastNumber = lastNumber; }
}
