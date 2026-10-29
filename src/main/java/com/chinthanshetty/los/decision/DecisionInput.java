package com.chinthanshetty.los.decision;

import java.math.BigDecimal;

/**
 * Everything the engine needs, already gathered from the applicant and external checks.
 * {@code creditScore} is null when the bureau could not be reached.
 */
public record DecisionInput(
        BigDecimal monthlyIncome,
        BigDecimal existingEmi,
        BigDecimal requestedAmount,
        int tenureMonths,
        Integer creditScore,
        boolean sanctionsMatch) {
}
