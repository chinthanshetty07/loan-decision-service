package com.chinthanshetty.los.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    Optional<LoanApplication> findByIdAndInstitutionCode(UUID id, String institutionCode);
}
