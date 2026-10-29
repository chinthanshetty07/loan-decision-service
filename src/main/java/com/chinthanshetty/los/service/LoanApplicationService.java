package com.chinthanshetty.los.service;

import com.chinthanshetty.los.decision.Decision;
import com.chinthanshetty.los.decision.DecisionEngine;
import com.chinthanshetty.los.decision.DecisionInput;
import com.chinthanshetty.los.domain.ApplicationStatus;
import com.chinthanshetty.los.domain.CreditPolicy;
import com.chinthanshetty.los.domain.LoanApplication;
import com.chinthanshetty.los.domain.LoanApplicationRepository;
import com.chinthanshetty.los.integration.CreditBureauGateway;
import com.chinthanshetty.los.integration.SanctionsScreeningClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs an application through sanctions screening, the credit bureau and the decision
 * engine. External calls happen outside any database transaction so a slow bureau
 * never holds a connection, and each step is written to the audit trail as it happens.
 */
@Service
public class LoanApplicationService {

    public static final String REASON_SEPARATOR = " | ";

    private final LoanApplicationRepository applications;
    private final CreditPolicyService policies;
    private final CreditBureauGateway bureau;
    private final SanctionsScreeningClient sanctions;
    private final DecisionEngine engine;
    private final AuditService audit;

    public LoanApplicationService(LoanApplicationRepository applications, CreditPolicyService policies,
                                  CreditBureauGateway bureau, SanctionsScreeningClient sanctions,
                                  DecisionEngine engine, AuditService audit) {
        this.applications = applications;
        this.policies = policies;
        this.bureau = bureau;
        this.sanctions = sanctions;
        this.engine = engine;
        this.audit = audit;
    }

    public SubmitResult submit(NewApplication cmd, Caller caller, String idempotencyKey) {
        Optional<LoanApplication> replay = findReplay(idempotencyKey, caller);
        if (replay.isPresent()) {
            return new SubmitResult(replay.get(), true);
        }

        CreditPolicy policy = policies.activePolicy(caller.institutionCode(), cmd.productType());
        String pan = cmd.pan().toUpperCase(Locale.ROOT);

        LoanApplication app;
        try {
            app = applications.save(new LoanApplication(idempotencyKey, caller.institutionCode(), cmd.productType(),
                    cmd.applicantName().trim(), mask(pan), sha256(pan), cmd.monthlyIncome(), cmd.existingEmi(),
                    cmd.requestedAmount(), cmd.tenureMonths(), caller.username()));
        } catch (DataIntegrityViolationException race) {
            // A concurrent retry with the same key won the insert; return its result.
            return findReplay(idempotencyKey, caller).map(existing -> new SubmitResult(existing, true))
                    .orElseThrow(() -> race);
        }
        audit.record(app.getId(), "APPLICATION_SUBMITTED", caller.username(),
                "product=%s amount=%s tenure=%d pan=%s".formatted(cmd.productType(),
                        cmd.requestedAmount().toPlainString(), cmd.tenureMonths(), app.getPanMasked()));

        boolean sanctionsMatch = sanctions.isPossibleMatch(cmd.applicantName());
        audit.record(app.getId(), "SANCTIONS_SCREENED", AuditService.SYSTEM, "possibleMatch=" + sanctionsMatch);

        Optional<Integer> score = bureau.fetchScore(pan);
        audit.record(app.getId(), score.isPresent() ? "BUREAU_SCORE_FETCHED" : "BUREAU_UNAVAILABLE",
                AuditService.SYSTEM, score.map(s -> "score=" + s).orElse("retries exhausted or circuit open"));

        Decision decision = engine.evaluate(new DecisionInput(cmd.monthlyIncome(), cmd.existingEmi(),
                cmd.requestedAmount(), cmd.tenureMonths(), score.orElse(null), sanctionsMatch), policy.rules());

        app.recordDecision(decision.status(), score.orElse(null), decision.proposedEmi(), decision.foir(),
                policy.getId(), String.join(REASON_SEPARATOR, decision.reasons()));
        app = applications.save(app);
        audit.record(app.getId(), "DECISION_MADE", AuditService.SYSTEM,
                "status=%s policy=%s v%d (id %d) reasons=%s".formatted(decision.status(), policy.getProductType(),
                        policy.getVersion(), policy.getId(), decision.reasons()));
        return new SubmitResult(app, false);
    }

    public LoanApplication get(UUID id, Caller caller) {
        return applications.findByIdAndInstitutionCode(id, caller.institutionCode())
                .orElseThrow(() -> new NotFoundException("Application " + id + " not found"));
    }

    public LoanApplication review(UUID id, ApplicationStatus outcome, String note, Caller caller) {
        LoanApplication app = get(id, caller);
        String reasons = app.getDecisionReasons() + REASON_SEPARATOR + "MANUAL_" + outcome + ": " + note.trim();
        app.resolveReview(outcome, reasons);
        app = applications.save(app);
        audit.record(app.getId(), "MANUAL_REVIEW_COMPLETED", caller.username(), "outcome=" + outcome + " note=" + note.trim());
        return app;
    }

    private Optional<LoanApplication> findReplay(String idempotencyKey, Caller caller) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return applications.findByIdempotencyKey(idempotencyKey)
                .filter(existing -> existing.getInstitutionCode().equals(caller.institutionCode()));
    }

    static String mask(String pan) {
        return "XXXXX" + pan.substring(5, 9).replaceAll("\\d(?=\\d{2})", "X") + pan.charAt(9);
    }

    // A production system would use a keyed HMAC so hashes can't be brute-forced from the PAN format.
    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
