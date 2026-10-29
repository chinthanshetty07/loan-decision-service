package com.chinthanshetty.los.web;

import com.chinthanshetty.los.domain.PolicyRules;
import com.chinthanshetty.los.domain.ProductType;
import com.chinthanshetty.los.service.Caller;
import com.chinthanshetty.los.service.CreditPolicyService;
import com.chinthanshetty.los.web.ApiModels.PolicyResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Credit rules for the caller's institution. Changing a rule needs no code change or redeploy. */
@RestController
@RequestMapping("/api/v1/policies")
public class CreditPolicyController {

    private final CreditPolicyService policies;

    public CreditPolicyController(CreditPolicyService policies) {
        this.policies = policies;
    }

    @GetMapping
    public List<PolicyResponse> active(@AuthenticationPrincipal Jwt jwt) {
        return policies.activePolicies(CallerResolver.from(jwt).institutionCode()).stream()
                .map(PolicyResponse::from).toList();
    }

    @GetMapping("/{productType}/history")
    public List<PolicyResponse> history(@PathVariable ProductType productType, @AuthenticationPrincipal Jwt jwt) {
        return policies.history(CallerResolver.from(jwt).institutionCode(), productType).stream()
                .map(PolicyResponse::from).toList();
    }

    @PutMapping("/{productType}")
    public PolicyResponse publish(@PathVariable ProductType productType, @Valid @RequestBody PolicyRules rules,
                                  @AuthenticationPrincipal Jwt jwt) {
        Caller caller = CallerResolver.from(jwt);
        return PolicyResponse.from(policies.publish(caller.institutionCode(), productType, rules, caller.username()));
    }
}
