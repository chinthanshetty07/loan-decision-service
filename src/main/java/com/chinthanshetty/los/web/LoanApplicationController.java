package com.chinthanshetty.los.web;

import com.chinthanshetty.los.service.AuditService;
import com.chinthanshetty.los.service.Caller;
import com.chinthanshetty.los.service.LoanApplicationService;
import com.chinthanshetty.los.service.SubmitResult;
import com.chinthanshetty.los.web.ApiModels.ApplicationResponse;
import com.chinthanshetty.los.web.ApiModels.AuditEventResponse;
import com.chinthanshetty.los.web.ApiModels.NewApplicationRequest;
import com.chinthanshetty.los.web.ApiModels.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class LoanApplicationController {

    private final LoanApplicationService service;
    private final AuditService audit;

    public LoanApplicationController(LoanApplicationService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    /** Submits and decides an application. Retrying with the same Idempotency-Key returns the original result. */
    @PostMapping
    public ResponseEntity<ApplicationResponse> submit(@Valid @RequestBody NewApplicationRequest request,
                                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                      @AuthenticationPrincipal Jwt jwt) {
        SubmitResult result = service.submit(request.toCommand(), CallerResolver.from(jwt), idempotencyKey);
        ApplicationResponse body = ApplicationResponse.from(result.application());
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replay", "true").body(body);
        }
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ApplicationResponse.from(service.get(id, CallerResolver.from(jwt)));
    }

    @GetMapping("/{id}/audit")
    public List<AuditEventResponse> auditTrail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Caller caller = CallerResolver.from(jwt);
        service.get(id, caller); // enforces institution scoping before exposing the trail
        return audit.trail(id).stream().map(AuditEventResponse::from).toList();
    }

    @PostMapping("/{id}/review")
    public ApplicationResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return ApplicationResponse.from(service.review(id, request.outcome(), request.note(), CallerResolver.from(jwt)));
    }
}
