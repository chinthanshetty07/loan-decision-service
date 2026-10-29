package com.chinthanshetty.los.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("los.security")
public record SecurityProperties(String jwtSecret, Duration tokenTtl, List<DemoUser> users) {

    public record DemoUser(String username, String password, String institution, List<String> roles) {
    }
}
