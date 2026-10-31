package com.eskcti.algashop.billing.utils;

import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockJwtDecoderFactory {

    public static final String DEFAULT_ISSUER_URI = "http://auth.algashop.local:8081";

    public static final String[] DEFAULT_SCOPES = new String[] {
            "invoices:read",
            "invoices:write",
            "credit-cards:read",
            "credit-cards:write",
    };

    public static final String DEFAULT_SUBJECT = "00000000-0000-0000-0000-000000000001";

    public static final String DEFAULT_ROLE = "MANAGER";

    public static final String CUSTOMER_ROLE = "CUSTOMER";

    public static final String MACHINE_SUBJECT = "algashop-billing-service";

    public static final String DEFAULT_TOKEN_VALUE = "fake.jwt.token";

    public static final String CUSTOMER_TOKEN_VALUE = "fake.customer.jwt.token";

    public static final String MACHINE_TOKEN_VALUE = "fake.machine.jwt.token";

    public static JwtDecoder createMockJwtDecoder() {
        JwtDecoder jwtDecoder = Mockito.mock(JwtDecoder.class);
        Mockito.when(jwtDecoder.decode(Mockito.anyString()))
                .thenAnswer(invocation -> buildJwtForToken(invocation.getArgument(0)));
        return jwtDecoder;
    }

    public static Jwt buildJwtForToken(String tokenValue) {
        if (MACHINE_TOKEN_VALUE.equals(tokenValue)) {
            return buildJwt(MACHINE_SUBJECT, DEFAULT_ISSUER_URI, DEFAULT_SCOPES, null, List.of(MACHINE_SUBJECT));
        }
        if (CUSTOMER_TOKEN_VALUE.equals(tokenValue)) {
            return buildJwt(DEFAULT_SUBJECT, DEFAULT_ISSUER_URI, DEFAULT_SCOPES, CUSTOMER_ROLE);
        }
        return buildJwt(DEFAULT_SUBJECT, DEFAULT_ISSUER_URI, DEFAULT_SCOPES, DEFAULT_ROLE);
    }

    public static JwtDecoder createMockJwtDecoder(String subject, String issuer, String[] scopes) {
        return createMockJwtDecoder(subject, issuer, scopes, DEFAULT_ROLE);
    }

    public static JwtDecoder createMockJwtDecoder(String subject, String issuer, String[] scopes, String role) {
        JwtDecoder jwtDecoder = Mockito.mock(JwtDecoder.class);
        Mockito.when(jwtDecoder.decode(Mockito.anyString()))
                .thenReturn(buildJwt(subject, issuer, scopes, role));
        return jwtDecoder;
    }

    public static Jwt buildJwt(String subject, String issuer, String[] scopes) {
        return buildJwt(subject, issuer, scopes, DEFAULT_ROLE);
    }

    public static Jwt buildJwt(String subject, String issuer, String[] scopes, String role) {
        return buildJwt(subject, issuer, scopes, role, List.of("algashop-api"));
    }

    public static Jwt buildJwt(String subject, String issuer, String[] scopes, String role, List<String> audience) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(600);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("iss", issuer);
        claims.put("aud", audience);

        if (scopes != null && scopes.length > 0) {
            claims.put("scope", String.join(" ", scopes));
        }

        if (role != null && !role.isBlank()) {
            claims.put("role", role);
        }

        return Jwt.withTokenValue(DEFAULT_TOKEN_VALUE)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .claims(c -> c.putAll(claims))
                .headers(h -> h.put("alg", "none"))
                .build();
    }

}
