package com.chinthanshetty.los.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String idempotencyKey;

    @Column(nullable = false)
    private String institutionCode;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private String applicantName;
    /** Only the masked PAN and a hash are stored; the raw PAN never reaches the database. */
    private String panMasked;
    private String panHash;
    private BigDecimal monthlyIncome;
    private BigDecimal existingEmi;
    private BigDecimal requestedAmount;
    private int tenureMonths;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private Integer creditScore;
    private BigDecimal proposedEmi;
    private BigDecimal foir;
    private Long policyId;
    private String decisionReasons;
    private String createdBy;
    private Instant createdAt;
    private Instant decidedAt;

    protected LoanApplication() {
    }

    public LoanApplication(String idempotencyKey, String institutionCode, ProductType productType,
                           String applicantName, String panMasked, String panHash,
                           BigDecimal monthlyIncome, BigDecimal existingEmi, BigDecimal requestedAmount,
                           int tenureMonths, String createdBy) {
        this.idempotencyKey = idempotencyKey;
        this.institutionCode = institutionCode;
        this.productType = productType;
        this.applicantName = applicantName;
        this.panMasked = panMasked;
        this.panHash = panHash;
        this.monthlyIncome = monthlyIncome;
        this.existingEmi = existingEmi;
        this.requestedAmount = requestedAmount;
        this.tenureMonths = tenureMonths;
        this.createdBy = createdBy;
        this.status = ApplicationStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    public void recordDecision(ApplicationStatus status, Integer creditScore, BigDecimal proposedEmi,
                               BigDecimal foir, Long policyId, String reasons) {
        this.status = status;
        this.creditScore = creditScore;
        this.proposedEmi = proposedEmi;
        this.foir = foir;
        this.policyId = policyId;
        this.decisionReasons = reasons;
        this.decidedAt = Instant.now();
    }

    public void resolveReview(ApplicationStatus outcome, String reasons) {
        if (status != ApplicationStatus.REFERRED) {
            throw new IllegalStateException("Only REFERRED applications can be reviewed; this one is " + status);
        }
        if (!outcome.isFinal()) {
            throw new IllegalArgumentException("Review outcome must be APPROVED or REJECTED");
        }
        this.status = outcome;
        this.decisionReasons = reasons;
        this.decidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public ProductType getProductType() {
        return productType;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getPanMasked() {
        return panMasked;
    }

    public String getPanHash() {
        return panHash;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public BigDecimal getExistingEmi() {
        return existingEmi;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public BigDecimal getProposedEmi() {
        return proposedEmi;
    }

    public BigDecimal getFoir() {
        return foir;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getDecisionReasons() {
        return decisionReasons;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
