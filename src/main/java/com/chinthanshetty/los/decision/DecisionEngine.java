package com.chinthanshetty.los.decision;

import com.chinthanshetty.los.domain.ApplicationStatus;
import com.chinthanshetty.los.domain.PolicyRules;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure rule evaluation: no I/O, no clock, so every decision can be replayed from the
 * stored inputs and the policy version it was made under.
 *
 * <p>Order of precedence:
 * <ol>
 *   <li>Any hard rule failing rejects the application, and every failing rule is reported.</li>
 *   <li>A possible sanctions match is never auto-approved and goes to compliance review.</li>
 *   <li>A missing bureau score is referred rather than guessed.</li>
 *   <li>A score inside the refer band above the minimum is referred to an underwriter.</li>
 *   <li>Otherwise the application is approved.</li>
 * </ol>
 */
@Component
public class DecisionEngine {

    public Decision evaluate(DecisionInput in, PolicyRules rules) {
        BigDecimal emi = EmiCalculator.monthlyEmi(in.requestedAmount(), rules.annualInterestRate(), in.tenureMonths());
        BigDecimal foir = in.existingEmi().add(emi).divide(in.monthlyIncome(), 4, RoundingMode.HALF_UP);

        List<String> rejections = new ArrayList<>();
        if (in.creditScore() != null && in.creditScore() < rules.minCreditScore()) {
            rejections.add("SCORE_BELOW_MIN: " + in.creditScore() + " < " + rules.minCreditScore());
        }
        if (in.monthlyIncome().compareTo(rules.minMonthlyIncome()) < 0) {
            rejections.add("INCOME_BELOW_MIN: " + plain(in.monthlyIncome()) + " < " + plain(rules.minMonthlyIncome()));
        }
        if (in.requestedAmount().compareTo(rules.maxLoanAmount()) > 0) {
            rejections.add("AMOUNT_ABOVE_MAX: " + plain(in.requestedAmount()) + " > " + plain(rules.maxLoanAmount()));
        }
        if (in.tenureMonths() > rules.maxTenureMonths()) {
            rejections.add("TENURE_ABOVE_MAX: " + in.tenureMonths() + " > " + rules.maxTenureMonths());
        }
        if (foir.compareTo(rules.maxFoir()) > 0) {
            rejections.add("FOIR_ABOVE_MAX: " + plain(foir) + " > " + plain(rules.maxFoir()));
        }

        List<String> referrals = new ArrayList<>();
        if (in.sanctionsMatch()) {
            referrals.add("SANCTIONS_POSSIBLE_MATCH: compliance review required");
        }
        if (in.creditScore() == null) {
            referrals.add("BUREAU_UNAVAILABLE: credit score could not be fetched");
        } else if (rejections.isEmpty() && in.creditScore() < rules.minCreditScore() + rules.referBand()) {
            referrals.add("SCORE_IN_REVIEW_BAND: " + in.creditScore() + " < "
                    + (rules.minCreditScore() + rules.referBand()));
        }

        if (in.sanctionsMatch()) {
            List<String> all = new ArrayList<>(referrals);
            all.addAll(rejections);
            return new Decision(ApplicationStatus.REFERRED, all, emi, foir);
        }
        if (!rejections.isEmpty()) {
            return new Decision(ApplicationStatus.REJECTED, rejections, emi, foir);
        }
        if (!referrals.isEmpty()) {
            return new Decision(ApplicationStatus.REFERRED, referrals, emi, foir);
        }
        return new Decision(ApplicationStatus.APPROVED, List.of("ALL_RULES_PASSED"), emi, foir);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
