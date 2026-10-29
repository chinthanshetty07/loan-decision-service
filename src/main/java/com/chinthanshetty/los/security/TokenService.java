package com.chinthanshetty.los.security;

import com.chinthanshetty.los.security.SecurityProperties.DemoUser;
import com.chinthanshetty.los.web.ApiModels.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TokenService {

    static final String ISSUER = "loan-decision-service";

    private final JwtEncoder encoder;
    private final PasswordEncoder passwords;
    private final SecurityProperties props;
    private final Map<String, DemoUser> users;
    private final Map<String, String> passwordHashes;

    public TokenService(JwtEncoder encoder, PasswordEncoder passwords, SecurityProperties props) {
        this.encoder = encoder;
        this.passwords = passwords;
        this.props = props;
        this.users = props.users().stream().collect(Collectors.toMap(DemoUser::username, Function.identity()));
        // Hash once at startup so plain-text demo passwords are never compared directly.
        this.passwordHashes = props.users().stream()
                .collect(Collectors.toMap(DemoUser::username, u -> passwords.encode(u.password())));
    }

    public TokenResponse issue(String username, String password) {
        DemoUser user = users.get(username);
        if (user == null || !passwords.matches(password, passwordHashes.get(username))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(now.plus(props.tokenTtl()))
                .claim("institution", user.institution())
                .claim("roles", user.roles())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", props.tokenTtl().toSeconds(), user.institution(), user.roles());
    }
}
