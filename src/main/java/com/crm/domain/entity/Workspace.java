package com.crm.domain.entity;

import com.crm.domain.enums.WorkspaceTaxStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workspaces")
@EntityListeners(AuditingEntityListener.class)
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToMany
    @JoinTable(
            name = "workspace_members",
            joinColumns = @JoinColumn(name = "workspace_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    /** Business tax status of the workspace's own company (Billing & Documents addon). Drives which
     * tax document types conversion may offer and whether VAT/allocation numbers apply. Null until the
     * workspace configures its billing profile. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", length = 20)
    private WorkspaceTaxStatus taxStatus;

    /** Legal business name shown on generated PDF headers, if different from {@link #name}. */
    @Column(name = "legal_name")
    private String legalName;

    /** ת"ז/ח"פ of the workspace's own business (the issuer of billing documents). */
    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(name = "business_address")
    private String businessAddress;

    @Column(name = "business_email")
    private String businessEmail;

    @Column(name = "business_phone")
    private String businessPhone;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public static String slugFrom(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        return s.isEmpty() ? "workspace" : s;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { this.members = members; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public WorkspaceTaxStatus getTaxStatus() { return taxStatus; }
    public void setTaxStatus(WorkspaceTaxStatus taxStatus) { this.taxStatus = taxStatus; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }
    public String getBusinessPhone() { return businessPhone; }
    public void setBusinessPhone(String businessPhone) { this.businessPhone = businessPhone; }
}
