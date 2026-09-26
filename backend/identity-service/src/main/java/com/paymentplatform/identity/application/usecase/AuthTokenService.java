package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.port.TokenIssuer;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Construction unique de la paire access+refresh au format {@link LoginResponse}.
 * Factorise le code partagé par login password, OAuth et dev-login (aucune duplication).
 */
@Service
public class AuthTokenService {

    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokens;

    public AuthTokenService(TokenIssuer tokenIssuer, RefreshTokenService refreshTokens) {
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
    }

    /**
     * Émet la paire access/refresh pour un utilisateur déjà vérifié.
     *
     * @param user utilisateur actif vérifié
     * @return paire au format login actuel
     */
    public LoginResponse issueTokens(User user) {
        List<String> roles = user.roles().stream().map(Enum::name).toList();
        var authenticated = new AuthenticatedUser(user.id().value(), user.username().value(), roles,
                user.organizationId() == null ? null : user.organizationId().value());
        String accessToken = tokenIssuer.issue(authenticated);
        var refresh = refreshTokens.issue(user.id().value());
        return LoginResponse.of(accessToken, tokenIssuer.expirationSeconds(), UserResponse.from(user),
                refresh.rawToken(), refresh.expiresInSeconds());
    }
}
