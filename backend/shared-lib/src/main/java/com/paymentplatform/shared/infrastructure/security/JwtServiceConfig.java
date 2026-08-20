package com.paymentplatform.shared.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans JWT partagés, actifs pour tous les services y compris le Gateway. */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class JwtServiceConfig {

    @Bean
    public JwtService jwtService(SecurityProperties properties) {
        return new JwtService(properties);
    }
}