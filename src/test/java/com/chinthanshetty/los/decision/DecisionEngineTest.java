package com.chinthanshetty.los.decision;

import com.chinthanshetty.los.domain.ApplicationStatus;
import com.chinthanshetty.los.domain.PolicyRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTest {

    private final DecisionEngine engine = new DecisionEngine();

    // min score 700, refer band 30, FOIR 50%, income >= 25k, amount <= 15L, tenure <= 60, 12.5%
    private final PolicyRules personal = new PolicyRules(700, 30, new BigDecimal("0.50"), new BigDecimal("25000"),
            new BigDecimal("1500000"), 60, new BigDecimal("12.50"));

    private DecisionInput input(String income, String existingEmi, String amount, int tenure, Integer score,
                                boolean sanctions) {
        return new DecisionInput(new BigDecimal(income), new BigDecimal(existingEmi), new BigDecimal(amount),
                tenure, score, sanctions);
    }

    @Test
    void approvesWhenEveryRulePasses() {
        Decision d = engine.evaluate(input("90000", "5000", "500000", 36, 780, false), personal);

        assertThat(d.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(d.proposedEmi()).isEqualByComparingTo("16726.81");
        assertThat(d.foir()).isEqualByComparingTo("0.2414");
    }

    @Test
    void rejectsAndReportsEveryFailingRule() {
        Decision d = engine.evaluate(input("20000", "8000", "2000000", 72, 640, false), personal);

        assertThat(d.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(d.reasons()).extracting(r -> r.substring(0, r.indexOf(':')))
                .containsExactly("SCORE_BELOW_MIN", "INCOME_BELOW_MIN", "AMOUNT_ABOVE_MAX",
                        "TENURE_ABOVE_MAX", "FOIR_ABOVE_MAX");
    }

    @Test
    void refersBorderlineScoreInsteadOfAutoApproving() {
        Decision d = engine.evaluate(input("90000", "0", "300000", 24, 715, false), personal);

        assertThat(d.status()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(d.reasons()).singleElement().asString().startsWith("SCORE_IN_REVIEW_BAND");
    }

    @Test
    void refersWhenBureauIsDownButOtherwiseEligible() {
        Decision d = engine.evaluate(input("90000", "0", "300000", 24, null, false), personal);

        assertThat(d.status()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(d.reasons()).singleElement().asString().startsWith("BUREAU_UNAVAILABLE");
    }

    @Test
    void stillRejectsOnHardRulesWhenBureauIsDown() {
        Decision d = engine.evaluate(input("15000", "0", "300000", 24, null, false), personal);

        assertThat(d.status()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void sanctionsMatchIsNeverAutoDecided() {
        Decision clean = engine.evaluate(input("90000", "0", "300000", 24, 850, true), personal);
        Decision failing = engine.evaluate(input("15000", "0", "300000", 24, 600, true), personal);

        assertThat(clean.status()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(failing.status()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(failing.reasons()).first().asString().startsWith("SANCTIONS_POSSIBLE_MATCH");
    }
}
