package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.Pattern;

public record UpdatePreferencesRequest(
        @Pattern(regexp = "fr|en", message = "Langue préférée invalide : fr ou en attendu") String preferredLang,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur accent1 invalide : hexadécimal #RRGGBB attendu") String accentColor1,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur accent2 invalide : hexadécimal #RRGGBB attendu") String accentColor2,
        Boolean tourSeen
) {}
