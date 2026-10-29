package com.chinthanshetty.los.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditPolicyRepository extends JpaRepository<CreditPolicy, Long> {

    Optional<CreditPolicy> findByInstitutionCodeAndProductTypeAndActiveTrue(String institutionCode, ProductType productType);

    List<CreditPolicy> findByInstitutionCodeAndActiveTrueOrderByProductType(String institutionCode);

    List<CreditPolicy> findByInstitutionCodeAndProductTypeOrderByVersionDesc(String institutionCode, ProductType productType);
}
