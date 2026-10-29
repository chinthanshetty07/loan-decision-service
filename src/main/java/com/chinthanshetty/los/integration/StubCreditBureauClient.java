package com.chinthanshetty.los.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Deterministic stand-in for a real bureau so demos and tests are repeatable.
 * The four digits of the PAN (ABCDE<b>0420</b>F) choose the score:
 * <ul>
 *   <li>0000-0600 &rarr; score = 300 + digits (e.g. 0420 &rarr; 720)</li>
 *   <li>9999 &rarr; simulated outage (throws, exercising retry and the circuit breaker)</li>
 *   <li>anything else &rarr; 900</li>
 * </ul>
 */
@Component
public class StubCreditBureauClient implements CreditBureauClient {

    private final Duration latency;

    public StubCreditBureauClient(@Value("${los.bureau.stub.latency:0ms}") Duration latency) {
        this.latency = latency;
    }

    @Override
    public int fetchScore(String pan) {
        simulateNetwork();
        int digits = Integer.parseInt(pan.substring(5, 9));
        if (digits == 9999) {
            throw new BureauUnavailableException("Bureau timed out for PAN ending " + pan.substring(6));
        }
        return digits <= 600 ? 300 + digits : 900;
    }

    private void simulateNetwork() {
        if (latency.isZero()) {
            return;
        }
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BureauUnavailableException("Interrupted while calling bureau");
        }
    }
}
