package com.chinthanshetty.los.web;

import com.chinthanshetty.los.domain.ApplicationStatus;
import com.chinthanshetty.los.domain.AuditEvent;
import com.chinthanshetty.los.domain.CreditPolicy;
import com.chinthanshetty.los.domain.LoanApplication;
import com.chinthanshetty.los.domain.PolicyRules;
import com.chinthanshetty.los.domain.ProductType;
import com.chinthanshetty.los.service.LoanApplicationService;
import com.chinthanshetty.los.service.NewApplication;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Request and response bodies for the public API, kept apart from the JPA entities. */
public final class ApiModels {

    private ApiModels() {
    }

    public record NewApplicationRequest(
            @NotNull ProductType productType,
            @NotBlank @Size(max = 120) String applicantName,
            @NotBlank @Pattern(regexp = "[A-Za-z]{5}[0-9]{4}[A-Za-z]", message = "must be a valid PAN, e.g. ABCDE1234F")
            String pan,
            @NotNull @Positive BigDecimal monthlyIncome,
            @NotNull @PositiveOrZero BigDecimal existingEmi,
            @NotNull @Positive BigDecimal requestedAmount,
            @Min(1) @Max(480) int tenureMonths) {

        public NewApplication toCommand() {
            return new NewApplication(productType, applicantName, pan, monthlyIncome, existingEmi,
                    requestedAmount, tenureMonths);
        }
    }

    public record ApplicationResponse(
            UUID id,
            String institutionCode,
            ProductType productType,
            String applicantName,
            String panMasked,
            BigDecimal requestedAmount,
            int tenureMonths,
            ApplicationStatus status,
            Integer creditScore,
            BigDecimal proposedEmi,
            BigDecimal foir,
            Long policyId,
            List<String> reasons,
            Instant createdAt,
            Instant decidedAt) {

        static ApplicationResponse from(LoanApplication a) {
            List<String> reasons = a.getDecisionReasons() == null ? List.of()
                    : Arrays.asList(a.getDecisionReasons().split(java.util.regex.Pattern.quote(
                    LoanApplicationService.REASON_SEPARATOR)));
            return new ApplicationResponse(a.getId(), a.getInstitutionCode(), a.getProductType(),
                    a.getApplicantName(), a.getPanMasked(), a.getRequestedAmount(), a.getTenureMonths(),
                    a.getStatus(), a.getCreditScore(), a.getProposedEmi(), a.getFoir(), a.getPolicyId(),
                    reasons, a.getCreatedAt(), a.getDecidedAt());
        }
    }

    public record ReviewRequest(
            @NotNull ApplicationStatus outcome,
            @NotBlank @Size(max = 500) String note) {
    }

    public record AuditEventResponse(String eventType, String actor, String detail, Instant at) {

        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getEventType(), e.getActor(), e.getDetail(), e.getCreatedAt());
        }
    }

    public record PolicyResponse(
            Long id,
            ProductType productType,
            int version,
            boolean active,
            PolicyRules rules,
            String createdBy,
            Instant createdAt) {

        static PolicyResponse from(CreditPolicy p) {
            return new PolicyResponse(p.getId(), p.getProductType(), p.getVersion(), p.isActive(), p.rules(),
                    p.getCreatedBy(), p.getCreatedAt());
        }
    }

    public record TokenRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds,
                                String institutionCode, List<String> roles) {
    }
}
