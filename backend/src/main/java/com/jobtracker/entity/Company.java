package com.jobtracker.entity;

import com.jobtracker.enums.CompanyApprovalStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 180)
    private String normalizedName;

    @Column(length = 18)
    private String cnpj;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 1000)
    private String reviewNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    protected Company() {}

    public Company(
            String name,
            String normalizedName,
            String cnpj,
            String website,
            String description,
            User creator) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.cnpj = cnpj;
        this.website = website;
        this.description = description;
        this.createdBy = creator;
        this.approvalStatus = CompanyApprovalStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(
            String name, String normalizedName, String cnpj, String website, String description) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.cnpj = cnpj;
        this.website = website;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void review(CompanyApprovalStatus status, User reviewer, String notes) {
        this.approvalStatus = status;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getWebsite() {
        return website;
    }

    public String getDescription() {
        return description;
    }

    public CompanyApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
