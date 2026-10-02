package com.eskcti.algashop.billing.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class MockJwtDecoderConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return MockJwtDecoderFactory.createMockJwtDecoder();
    }

}
