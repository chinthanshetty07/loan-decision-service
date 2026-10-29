package com.chinthanshetty.los.domain;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** The tunable part of a credit policy, shared by the entity, the API and the decision engine. */
public record PolicyRules(
        @Min(300) @Max(900) int minCreditScore,
        @Min(0) @Max(200) int referBand,
        @NotNull @DecimalMin("0.05") @DecimalMax("0.95") BigDecimal maxFoir,
        @NotNull @Positive BigDecimal minMonthlyIncome,
        @NotNull @Positive BigDecimal maxLoanAmount,
        @Min(1) @Max(480) int maxTenureMonths,
        @NotNull @DecimalMin("0.0") @DecimalMax("60.0") BigDecimal annualInterestRate) {
}
