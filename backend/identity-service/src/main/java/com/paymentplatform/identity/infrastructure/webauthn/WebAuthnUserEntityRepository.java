package com.paymentplatform.identity.infrastructure.webauthn;

import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.UserId;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptation du dépôt d'utilisateurs WebAuthn de Spring Security sur le
 * UserRepository du service. Le user handle est l'UUID textuel de l'utilisateur.
 */
@Component
public class WebAuthnUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final UserRepository users;

    public WebAuthnUserEntityRepository(UserRepository users) {
        this.users = users;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        return users.findById(UserId.of(WebAuthnSupport.userIdFromHandle(id)))
                .map(this::toUserEntity)
                .orElse(null);
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        return users.findByUsername(Username.of(username))
                .map(this::toUserEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        throw new UnsupportedOperationException(
                "L'enregistrement des utilisateurs WebAuthn passe par le UserRepository du service");
    }

    @Override
    public void delete(Bytes id) {
        throw new UnsupportedOperationException(
                "La suppression des utilisateurs WebAuthn passe par le cycle de vie du compte");
    }

    private PublicKeyCredentialUserEntity toUserEntity(com.paymentplatform.identity.domain.model.User user) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .name(user.username().value())
                .displayName((user.firstName() + " " + user.lastName()).trim())
                .id(WebAuthnSupport.userHandle(user.id().value()))
                .build();
    }
}
