package com.chinthanshetty.los.decision;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmiCalculatorTest {

    @Test
    void matchesStandardAmortisationFormula() {
        // 5,00,000 at 12.5% for 36 months: the textbook EMI is 16,726.81
        assertThat(EmiCalculator.monthlyEmi(new BigDecimal("500000"), new BigDecimal("12.50"), 36))
                .isEqualByComparingTo("16726.81");
    }

    @Test
    void zeroInterestSplitsPrincipalEvenly() {
        assertThat(EmiCalculator.monthlyEmi(new BigDecimal("120000"), BigDecimal.ZERO, 12))
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void rejectsNonPositiveTenure() {
        assertThatThrownBy(() -> EmiCalculator.monthlyEmi(BigDecimal.TEN, BigDecimal.ONE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
