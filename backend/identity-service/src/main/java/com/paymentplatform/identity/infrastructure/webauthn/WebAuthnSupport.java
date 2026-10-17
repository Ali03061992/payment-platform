package com.paymentplatform.identity.infrastructure.webauthn;

import org.springframework.security.web.webauthn.api.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Conversion entre user handle WebAuthn (octets) et identifiants internes. */
public final class WebAuthnSupport {

    private WebAuthnSupport() {
    }

    /** User handle = UTF-8 de l'UUID sous forme textuelle (stable et réversible). */
    public static Bytes userHandle(UUID userId) {
        return new Bytes(userId.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static UUID userIdFromHandle(Bytes handle) {
        if (handle == null) {
            throw new IllegalArgumentException("User handle WebAuthn manquant");
        }
        return UUID.fromString(new String(handle.getBytes(), StandardCharsets.UTF_8));
    }

    /**
     * Compteur de signature de l'authenticatorData WebAuthn (offset 32, 4 octets big-endian).
     * La signature elle-même est déjà vérifiée par webauthn4j avant cet appel.
     */
    public static long parseSignatureCount(byte[] authenticatorData) {
        if (authenticatorData == null || authenticatorData.length < 36) {
            return 0L;
        }
        long count = 0;
        for (int i = 32; i < 36; i++) {
            count = (count << 8) | (authenticatorData[i] & 0xFFL);
        }
        return count;
    }
}
