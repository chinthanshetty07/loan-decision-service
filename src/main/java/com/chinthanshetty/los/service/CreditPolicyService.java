package com.chinthanshetty.los.service;

import com.chinthanshetty.los.domain.CreditPolicy;
import com.chinthanshetty.los.domain.CreditPolicyRepository;
import com.chinthanshetty.los.domain.PolicyRules;
import com.chinthanshetty.los.domain.ProductType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreditPolicyService {

    private final CreditPolicyRepository policies;

    public CreditPolicyService(CreditPolicyRepository policies) {
        this.policies = policies;
    }

    @Transactional(readOnly = true)
    public CreditPolicy activePolicy(String institutionCode, ProductType productType) {
        return policies.findByInstitutionCodeAndProductTypeAndActiveTrue(institutionCode, productType)
                .orElseThrow(() -> new NoActivePolicyException(
                        "No active " + productType + " policy for " + institutionCode));
    }

    @Transactional(readOnly = true)
    public List<CreditPolicy> activePolicies(String institutionCode) {
        return policies.findByInstitutionCodeAndActiveTrueOrderByProductType(institutionCode);
    }

    @Transactional(readOnly = true)
    public List<CreditPolicy> history(String institutionCode, ProductType productType) {
        return policies.findByInstitutionCodeAndProductTypeOrderByVersionDesc(institutionCode, productType);
    }

    /**
     * Publishes a new version and retires the current one in one transaction. Applications
     * already decided keep pointing at the version they were decided under.
     */
    @Transactional
    public CreditPolicy publish(String institutionCode, ProductType productType, PolicyRules rules, String actor) {
        CreditPolicy current = policies.findByInstitutionCodeAndProductTypeAndActiveTrue(institutionCode, productType)
                .orElseThrow(() -> new NotFoundException(
                        "No " + productType + " policy to update for " + institutionCode));
        current.deactivate();
        policies.saveAndFlush(current);
        return policies.save(current.nextVersion(rules, actor));
    }
}
