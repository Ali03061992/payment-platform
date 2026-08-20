package com.paymentplatform.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Propriétés JWT : secret injecté par variable d'environnement (jamais en dur). */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String secret, Duration expiration) {

    public static final String DEFAULT_EXPIRATION = "30m";
}