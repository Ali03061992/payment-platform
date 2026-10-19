package com.paymentplatform.identity.interfaces.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Construit le cookie HttpOnly qui porte le refresh token pour la PWA web.
 * Attributs : HttpOnly (invisible au JS), SameSite=Strict (anti-CSRF),
 * Path=/api/auth (uniquement les endpoints de refresh/logout),
 * Secure (paramétrable via AUTH_COOKIE_SECURE, défaut true).
 * Le body JSON reste inchangé pour les clients mobiles (Flutter) —
 * le cookie est un canal additive pour le web uniquement.
 */
@Component
public class RefreshCookieSupport {

    public static final String COOKIE_NAME = "pp_rt";
    public static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;

    public RefreshCookieSupport(@Value("${app.auth.cookie-secure:true}") boolean secure) {
        this.secure = secure;
    }

    /** Cookie de refresh token avec expiration en secondes. */
    public ResponseCookie build(String rawToken, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(Math.max(1, maxAgeSeconds)))
                .build();
    }

    /** Cookie de suppression (Max-Age=0) pour le logout. */
    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
