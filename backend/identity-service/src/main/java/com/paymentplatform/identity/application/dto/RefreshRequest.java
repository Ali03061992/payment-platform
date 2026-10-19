package com.paymentplatform.identity.application.dto;

/**
 * Corps optionnel du refresh : le token peut aussi arriver en cookie HttpOnly
 * (pp_rt) pour le web — le body reste supporté pour le mobile Flutter.
 */
public record RefreshRequest(String refreshToken) {
}
