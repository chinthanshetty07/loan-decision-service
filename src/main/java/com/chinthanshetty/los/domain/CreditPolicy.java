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

/**
 * Per-institution, per-product credit rules. Rows are immutable once written;
 * {@link #nextVersion} creates the replacement when a risk admin changes a rule.
 */
@Entity
@Table(name = "credit_policy")
public class CreditPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String institutionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    private int version;
    private boolean active;
    private int minCreditScore;
    /** Scores within this many points above the minimum go to manual review instead of auto-approval. */
    private int referBand;
    /** Maximum fixed-obligation-to-income ratio (all EMIs / monthly income). */
    private BigDecimal maxFoir;
    private BigDecimal minMonthlyIncome;
    private BigDecimal maxLoanAmount;
    private int maxTenureMonths;
    private BigDecimal annualInterestRate;
    private String createdBy;
    private Instant createdAt;

    protected CreditPolicy() {
    }

    public CreditPolicy nextVersion(PolicyRules rules, String actor) {
        CreditPolicy next = new CreditPolicy();
        next.institutionCode = institutionCode;
        next.productType = productType;
        next.version = version + 1;
        next.active = true;
        next.minCreditScore = rules.minCreditScore();
        next.referBand = rules.referBand();
        next.maxFoir = rules.maxFoir();
        next.minMonthlyIncome = rules.minMonthlyIncome();
        next.maxLoanAmount = rules.maxLoanAmount();
        next.maxTenureMonths = rules.maxTenureMonths();
        next.annualInterestRate = rules.annualInterestRate();
        next.createdBy = actor;
        next.createdAt = Instant.now();
        return next;
    }

    public PolicyRules rules() {
        return new PolicyRules(minCreditScore, referBand, maxFoir, minMonthlyIncome,
                maxLoanAmount, maxTenureMonths, annualInterestRate);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public ProductType getProductType() {
        return productType;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
