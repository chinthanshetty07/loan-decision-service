package com.chinthanshetty.los.service;

import com.chinthanshetty.los.decision.DecisionEngine;
import com.chinthanshetty.los.domain.ApplicationStatus;
import com.chinthanshetty.los.domain.CreditPolicy;
import com.chinthanshetty.los.domain.LoanApplication;
import com.chinthanshetty.los.domain.LoanApplicationRepository;
import com.chinthanshetty.los.domain.PolicyRules;
import com.chinthanshetty.los.domain.ProductType;
import com.chinthanshetty.los.integration.CreditBureauGateway;
import com.chinthanshetty.los.integration.SanctionsScreeningClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanApplicationServiceTest {

    @Mock LoanApplicationRepository applications;
    @Mock CreditPolicyService policies;
    @Mock CreditBureauGateway bureau;
    @Mock SanctionsScreeningClient sanctions;
    @Mock AuditService audit;
    @Spy DecisionEngine engine = new DecisionEngine();
    @InjectMocks LoanApplicationService service;

    private final Caller officer = new Caller("officer", "DEMO_BANK");
    private final NewApplication cmd = new NewApplication(ProductType.PERSONAL, "Asha Rao", "abcde0480f",
            new BigDecimal("90000"), BigDecimal.ZERO, new BigDecimal("300000"), 24);

    @BeforeEach
    void policy() {
        CreditPolicy policy = mock(CreditPolicy.class);
        when(policy.rules()).thenReturn(new PolicyRules(700, 30, new BigDecimal("0.50"), new BigDecimal("25000"),
                new BigDecimal("1500000"), 60, new BigDecimal("12.50")));
        when(policy.getId()).thenReturn(1L);
        when(policies.activePolicy("DEMO_BANK", ProductType.PERSONAL)).thenReturn(policy);
        when(applications.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void referredWhenBureauUnavailable() {
        when(bureau.fetchScore("ABCDE0480F")).thenReturn(Optional.empty());

        LoanApplication app = service.submit(cmd, officer, null).application();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(app.getCreditScore()).isNull();
        verify(audit).record(any(), eq("BUREAU_UNAVAILABLE"), eq(AuditService.SYSTEM), anyString());
    }

    @Test
    void storesOnlyMaskedAndHashedPan() {
        when(bureau.fetchScore("ABCDE0480F")).thenReturn(Optional.of(780));

        LoanApplication app = service.submit(cmd, officer, null).application();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(app.getPanMasked()).isEqualTo("XXXXXXX80F");
        assertThat(app.getPanHash()).hasSize(64).doesNotContain("ABCDE0480F");
    }

    @Test
    void replaysExistingApplicationForSameIdempotencyKey() {
        LoanApplication existing = new LoanApplication("key-1", "DEMO_BANK", ProductType.PERSONAL, "Asha Rao",
                "XXXXXXX80F", "hash", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, 12, "officer");
        when(applications.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        SubmitResult result = service.submit(cmd, officer, "key-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.application()).isSameAs(existing);
        verify(bureau, never()).fetchScore(anyString());
    }
}
