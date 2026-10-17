package com.paymentplatform.identity.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

import java.util.Set;

/**
 * Configuration WebAuthn : relying party (RP ID, nom, origines autorisées),
 * opérations de vérification déléguées à webauthn4j via Spring Security, et
 * mixins Jackson pour la sérialisation JSON des options au format W3C.
 */
@Configuration
@EnableConfigurationProperties(WebAuthnProperties.class)
public class WebAuthnConfig {

    @Bean
    public PublicKeyCredentialRpEntity webAuthnRelyingParty(WebAuthnProperties properties) {
        return PublicKeyCredentialRpEntity.builder()
                .id(properties.rpId())
                .name(properties.rpName())
                .build();
    }

    @Bean
    public WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntityRepository,
            UserCredentialRepository userCredentialRepository,
            PublicKeyCredentialRpEntity relyingParty,
            WebAuthnProperties properties) {
        Set<String> origins = properties.allowedOrigins() == null
                ? Set.of()
                : Set.copyOf(properties.allowedOrigins());
        return new Webauthn4JRelyingPartyOperations(
                userEntityRepository, userCredentialRepository, relyingParty, origins);
    }

    /** Enregistre les mixins WebAuthn sur le mapppeur JSON utilisé par MVC. */
    @Bean
    public JsonMapperBuilderCustomizer webAuthnJacksonCustomizer() {
        return builder -> builder.addModule(new WebauthnJacksonModule());
    }
}
