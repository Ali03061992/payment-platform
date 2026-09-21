package com.paymentplatform.identity.infrastructure.security;

import com.paymentplatform.identity.application.port.TokenIssuer;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.shared.infrastructure.security.JwtService;
import org.springframework.stereotype.Component;

@Component
public class IdentityTokenIssuer implements TokenIssuer {

    private final JwtService jwtService;

    public IdentityTokenIssuer(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String issue(AuthenticatedUser user) {
        return jwtService.issue(user);
    }

    @Override
    public long expirationSeconds() {
        return jwtService.expirationSeconds();
    }
}