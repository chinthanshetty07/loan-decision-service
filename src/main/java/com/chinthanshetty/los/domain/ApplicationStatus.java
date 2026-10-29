package com.chinthanshetty.los.domain;

public enum ApplicationStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    /** Needs a human decision: borderline score, bureau outage or a possible sanctions match. */
    REFERRED;

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED;
    }
}
