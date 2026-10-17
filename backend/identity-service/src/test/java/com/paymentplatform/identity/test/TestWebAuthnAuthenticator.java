package com.paymentplatform.identity.test;

import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

/**
 * Authenticant WebAuthn de test : génère des attestations "none" (CBOR) et des
 * assertions ES256 valides pour les options émises par le backend. Suffisant
 * pour passer la vérification webauthn4j en environnement H2 (RP ID localhost,
 * origine http://localhost:4200).
 */
public final class TestWebAuthnAuthenticator {

    private static final String ORIGIN = "http://localhost:4200";

    private final KeyPair keyPair;
    private final byte[] credentialId;
    private long signCount = 1;

    public TestWebAuthnAuthenticator() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            this.keyPair = generator.generateKeyPair();
            this.credentialId = new byte[32];
            new java.security.SecureRandom().nextBytes(credentialId);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] credentialId() {
        return credentialId;
    }

    /** Répond à PublicKeyCredentialCreationOptions avec une attestation "none". */
    public PublicKeyCredential<AuthenticatorAttestationResponse> create(
            PublicKeyCredentialCreationOptions options) {
        try {
            byte[] challenge = options.getChallenge().getBytes();
            byte[] clientDataJson = clientDataJson("webauthn.create", challenge);
            byte[] rpIdHash = sha256(options.getRp().getId().getBytes(StandardCharsets.UTF_8));
            byte[] coseKey = coseKey((ECPublicKey) keyPair.getPublic());
            byte[] attestedCredentialData = concat(
                    new byte[16],
                    shortBytes(credentialId.length),
                    credentialId,
                    coseKey);
            byte[] authData = concat(rpIdHash,
                    new byte[]{(byte) 0x45},
                    intBytes((int) signCount),
                    attestedCredentialData);
            byte[] attestationObject = attestationNone(authData);

            AuthenticatorAttestationResponse response = AuthenticatorAttestationResponse.builder()
                    .clientDataJSON(new Bytes(clientDataJson))
                    .attestationObject(new Bytes(attestationObject))
                    .transports(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.USB)
                    .build();
            return PublicKeyCredential.<AuthenticatorAttestationResponse>builder()
                    .id(new Bytes(credentialId).toBase64UrlString())
                    .type(org.springframework.security.web.webauthn.api.PublicKeyCredentialType.PUBLIC_KEY)
                    .rawId(new Bytes(credentialId))
                    .response(response)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Création d'attestation de test échouée", e);
        }
    }

    /** Répond à PublicKeyCredentialRequestOptions avec une assertion ES256. */
    public PublicKeyCredential<AuthenticatorAssertionResponse> get(
            PublicKeyCredentialRequestOptions options, byte[] userHandle) {
        try {
            byte[] challenge = options.getChallenge().getBytes();
            byte[] clientDataJson = clientDataJson("webauthn.get", challenge);
            byte[] rpIdHash = sha256(options.getRpId().getBytes(StandardCharsets.UTF_8));
            signCount++;
            byte[] authData = concat(rpIdHash, new byte[]{(byte) 0x05}, intBytes((int) signCount));

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(concat(authData, sha256(clientDataJson)));
            byte[] sig = signature.sign();

            AuthenticatorAssertionResponse response = AuthenticatorAssertionResponse.builder()
                    .clientDataJSON(new Bytes(clientDataJson))
                    .authenticatorData(new Bytes(authData))
                    .signature(new Bytes(sig))
                    .userHandle(new Bytes(userHandle))
                    .build();
            return PublicKeyCredential.<AuthenticatorAssertionResponse>builder()
                    .id(new Bytes(credentialId).toBase64UrlString())
                    .type(org.springframework.security.web.webauthn.api.PublicKeyCredentialType.PUBLIC_KEY)
                    .rawId(new Bytes(credentialId))
                    .response(response)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Création d'assertion de test échouée", e);
        }
    }

    private static byte[] clientDataJson(String type, byte[] challenge) {
        String json = "{\"type\":\"" + type + "\",\"challenge\":\""
                + new Bytes(challenge).toBase64UrlString()
                + "\",\"origin\":\"" + ORIGIN + "\",\"crossOrigin\":false}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** attestationObject CBOR {fmt:"none", attStmt:{}, authData:bytes}. */
    private static byte[] attestationNone(byte[] authData) {
        byte[] keyFmt = textPair("fmt", "none");
        byte[] keyAttStmt = textPair("attStmt", "");
        // "attStmt" -> map(0)
        byte[] attStmt = replaceValue(keyAttStmt, new byte[]{(byte) 0xA0});
        byte[] keyAuthData = textKey("authData");
        byte[] authDataValue = concat(
                authData.length < 24
                        ? new byte[]{(byte) (0x40 | authData.length)}
                        : new byte[]{(byte) 0x58, (byte) authData.length},
                authData);
        byte[] body = concat(keyFmt, attStmt, keyAuthData, authDataValue);
        return concat(new byte[]{(byte) (0xA0 | 3)}, body);
    }

    /** COSE_Key EC2 P-256 : {1:2, 3:-7, -1:1, -2:x, -3:y}. */
    private static byte[] coseKey(ECPublicKey publicKey) {
        BigInteger x = publicKey.getW().getAffineX();
        BigInteger y = publicKey.getW().getAffineY();
        byte[] xb = leftPad(x.toByteArray(), 32);
        byte[] yb = leftPad(y.toByteArray(), 32);
        return concat(
                new byte[]{(byte) 0xA0 | 5},
                new byte[]{0x01, 0x02},
                new byte[]{0x03, 0x26},
                new byte[]{0x20, 0x01},
                new byte[]{0x21, 0x58, 0x20}, xb,
                new byte[]{0x22, 0x58, 0x20}, yb);
    }

    private static byte[] textPair(String key, String value) {
        byte[] k = textKey(key);
        byte[] v = value.isEmpty()
                ? new byte[]{(byte) 0x60}
                : concat(new byte[]{(byte) (0x60 | value.length())},
                value.getBytes(StandardCharsets.US_ASCII));
        return concat(k, v);
    }

    private static byte[] textKey(String key) {
        return concat(new byte[]{(byte) (0x60 | key.length())},
                key.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] replaceValue(byte[] pair, byte[] newValue) {
        int keyLen = 1 + (pair[0] & 0x1F);
        return concat(Arrays.copyOfRange(pair, 0, keyLen), newValue);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] leftPad(byte[] bytes, int size) {
        if (bytes.length == size) {
            return bytes;
        }
        byte[] out = new byte[size];
        if (bytes.length > size) {
            // BigInteger.toByteArray ajoute un 0x00 de signe quand le bit de poids
            // fort est mis : on garde les `size` octets de poids faible.
            System.arraycopy(bytes, bytes.length - size, out, 0, size);
        } else {
            System.arraycopy(bytes, 0, out, size - bytes.length, bytes.length);
        }
        return out;
    }

    private static byte[] shortBytes(int value) {
        return new byte[]{(byte) (value >> 8), (byte) value};
    }

    private static byte[] intBytes(int value) {
        return new byte[]{(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }

    private static byte[] concat(byte[]... arrays) {
        int length = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] out = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, out, offset, array.length);
            offset += array.length;
        }
        return out;
    }
}
