package com.chinthanshetty.los.web;

import com.chinthanshetty.los.service.Caller;
import org.springframework.security.oauth2.jwt.Jwt;

final class CallerResolver {

    private CallerResolver() {
    }

    static Caller from(Jwt jwt) {
        return new Caller(jwt.getSubject(), jwt.getClaimAsString("institution"));
    }
}
