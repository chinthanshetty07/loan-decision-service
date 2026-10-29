package com.chinthanshetty.los.integration;

/** Port for a credit bureau such as CIBIL or Experian. */
public interface CreditBureauClient {

    /** @throws BureauUnavailableException when the bureau times out or errors */
    int fetchScore(String pan);
}
