package com.chinthanshetty.los.web;

import com.chinthanshetty.los.security.TokenService;
import com.chinthanshetty.los.web.ApiModels.TokenRequest;
import com.chinthanshetty.los.web.ApiModels.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issues demo JWTs. In a bank deployment this would be the bank's identity provider instead. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TokenService tokens;

    public AuthController(TokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return tokens.issue(request.username(), request.password());
    }
}
