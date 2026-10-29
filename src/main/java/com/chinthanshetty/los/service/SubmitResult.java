package com.chinthanshetty.los.service;

import com.chinthanshetty.los.domain.LoanApplication;

/** {@code replayed} is true when an Idempotency-Key matched an earlier submission. */
public record SubmitResult(LoanApplication application, boolean replayed) {
}
