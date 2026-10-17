package com.paymentplatform.notification.infrastructure.push;

import com.paymentplatform.notification.domain.model.VapidKeyPair;
import com.paymentplatform.notification.domain.model.VapidKeyPairRepository;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * Fournit la paire de clés VAPID. Property-first (app.webpush.vapid-public-key /
 * vapid-private-key) ; sinon charge la dernière paire persistée, sinon en génère
 * une et la sauvegarde (dev). Aucun secret n'est commité en dur.
 */
@Component
public class VapidKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(VapidKeyProvider.class);

    private final VapidKeyPairRepository repository;
    private final String configuredPublicKey;
    private final String configuredPrivateKey;

    private volatile KeyPair cached;

    public VapidKeyProvider(VapidKeyPairRepository repository,
                            @Value("${app.webpush.vapid-public-key:}") String configuredPublicKey,
                            @Value("${app.webpush.vapid-private-key:}") String configuredPrivateKey) {
        this.repository = repository;
        this.configuredPublicKey = configuredPublicKey == null ? "" : configuredPublicKey.trim();
        this.configuredPrivateKey = configuredPrivateKey == null ? "" : configuredPrivateKey.trim();
    }

    public String publicKeyBase64Url() {
        return encodePublicKey(getKeyPair().getPublic());
    }

    public KeyPair getKeyPair() {
        if (!configuredPublicKey.isBlank() && !configuredPrivateKey.isBlank()) {
            if (cached == null) {
                synchronized (this) {
                    if (cached == null) {
                        cached = new KeyPair(decodePublicKey(configuredPublicKey),
                                decodePrivateKey(configuredPrivateKey));
                    }
                }
            }
            return cached;
        }
        return loadOrGenerate();
    }

    @Transactional
    private synchronized KeyPair loadOrGenerate() {
        if (cached != null) {
            return cached;
        }
        var stored = repository.findFirstByOrderByCreatedAtDesc();
        if (stored.isPresent()) {
            log.info("VAPID keypair loaded from database");
            cached = new KeyPair(decodePublicKey(stored.get().publicKey()),
                    decodePrivateKey(stored.get().privateKey()));
            return cached;
        }
        try {
            KeyPair keyPair = generate();
            repository.save(new VapidKeyPair(encodePublicKey(keyPair.getPublic()),
                    encodePrivateKey(keyPair.getPrivate())));
            log.warn("VAPID keypair generated and persisted — configure app.webpush.vapid-* keys "
                    + "explicitly for production");
            cached = keyPair;
            return cached;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer la paire VAPID", e);
        }
    }

    private KeyPair generate() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        return generator.generateKeyPair();
    }

    private String encodePublicKey(PublicKey publicKey) {
        org.bouncycastle.jce.interfaces.ECPublicKey ecKey = (org.bouncycastle.jce.interfaces.ECPublicKey) publicKey;
        ECPoint point = ecKey.getQ().normalize();
        byte[] encoded = new byte[65];
        encoded[0] = 0x04;
        byte[] x = point.getAffineXCoord().getEncoded();
        byte[] y = point.getAffineYCoord().getEncoded();
        System.arraycopy(x, 0, encoded, 1 + (32 - x.length), x.length);
        System.arraycopy(y, 0, encoded, 33 + (32 - y.length), y.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    private String encodePrivateKey(PrivateKey privateKey) {
        BigInteger d = ((org.bouncycastle.jce.interfaces.ECPrivateKey) privateKey).getD();
        byte[] bytes = d.toByteArray();
        byte[] fixed = new byte[32];
        int offset = Math.max(0, bytes.length - 32);
        int length = Math.min(32, bytes.length);
        System.arraycopy(bytes, offset, fixed, 32 - length, length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fixed);
    }

    private PublicKey decodePublicKey(String base64Url) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
            ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
            ECPoint point = spec.getCurve().createPoint(
                    new BigInteger(1, java.util.Arrays.copyOfRange(decoded, 1, 33)),
                    new BigInteger(1, java.util.Arrays.copyOfRange(decoded, 33, 65)));
            KeyFactory factory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return factory.generatePublic(new ECPublicKeySpec(point, spec));
        } catch (Exception e) {
            throw new IllegalArgumentException("Clé publique VAPID invalide", e);
        }
    }

    private PrivateKey decodePrivateKey(String base64Url) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
            ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyFactory factory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return factory.generatePrivate(new ECPrivateKeySpec(new BigInteger(1, decoded), spec));
        } catch (Exception e) {
            throw new IllegalArgumentException("Clé privée VAPID invalide", e);
        }
    }
}
