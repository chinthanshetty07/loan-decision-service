package com.chinthanshetty.los.decision;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Standard reducing-balance EMI: P * r * (1 + r)^n / ((1 + r)^n - 1), with r the monthly rate. */
public final class EmiCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);

    private EmiCalculator() {
    }

    public static BigDecimal monthlyEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }
        if (annualRatePercent.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal r = annualRatePercent.divide(MONTHS_TIMES_PERCENT, MC);
        BigDecimal growth = BigDecimal.ONE.add(r).pow(tenureMonths, MC);
        return principal.multiply(r, MC)
                .multiply(growth, MC)
                .divide(growth.subtract(BigDecimal.ONE), MC)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
