package com.chinthanshetty.los.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Wraps the bureau with retry and a circuit breaker. A failed lookup returns empty
 * instead of an exception, so the application is referred for manual review rather
 * than lost.
 */
@Service
public class CreditBureauGateway {

    private static final Logger log = LoggerFactory.getLogger(CreditBureauGateway.class);

    private final CreditBureauClient client;

    public CreditBureauGateway(CreditBureauClient client) {
        this.client = client;
    }

    // Retry is the outer aspect, so the fallback lives there and only runs after all attempts fail.
    @Retry(name = "creditBureau", fallbackMethod = "unavailable")
    @CircuitBreaker(name = "creditBureau")
    public Optional<Integer> fetchScore(String pan) {
        return Optional.of(client.fetchScore(pan));
    }

    @SuppressWarnings("unused")
    private Optional<Integer> unavailable(String pan, Throwable cause) {
        log.warn("Credit bureau unavailable, referring application: {}", cause.toString());
        return Optional.empty();
    }
}
