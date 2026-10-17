package com.paymentplatform.identity.infrastructure.configuration;

import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;

/**
 * Codec JSON dédié aux options et credentials WebAuthn (Jackson 3 + mixins
 * Spring Security). Utilisé pour persister les options émises (rejeu exact de
 * la vérification) et pour extraire le challenge du clientDataJSON.
 */
@Component
public class WebAuthnJsonCodec {

    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new WebauthnJacksonModule())
            .build();

    public String write(PublicKeyCredentialCreationOptions options) {
        return mapper.writeValueAsString(options);
    }

    public String write(PublicKeyCredentialRequestOptions options) {
        return mapper.writeValueAsString(options);
    }

    public PublicKeyCredentialCreationOptions readCreationOptions(String json) {
        return mapper.readValue(json, PublicKeyCredentialCreationOptions.class);
    }

    public PublicKeyCredentialRequestOptions readRequestOptions(String json) {
        return mapper.readValue(json, PublicKeyCredentialRequestOptions.class);
    }

    /** Extrait le challenge (base64url) du clientDataJSON / attestationClientDataJSON. */
    public Bytes challengeFromClientData(Bytes clientDataJson) {
        try {
            JsonNode node = mapper.readTree(clientDataJson.getBytes());
            String challenge = node.get("challenge").asText();
            return new Bytes(Base64.getUrlDecoder().decode(challenge));
        } catch (Exception e) {
            throw new IllegalArgumentException("clientDataJSON illisible", e);
        }
    }
}
