package com.chinthanshetty.los.integration;

/** Port for sanctions / watchlist screening (OFAC, UN, domestic lists). */
public interface SanctionsScreeningClient {

    boolean isPossibleMatch(String fullName);
}
