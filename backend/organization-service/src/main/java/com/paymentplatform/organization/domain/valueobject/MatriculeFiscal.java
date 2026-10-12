package com.paymentplatform.organization.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.util.regex.Pattern;

/**
 * Matricule fiscal tunisien (carte d'identification fiscale).
 * Format général : 7-8 chiffres + code TVA (A standard, P partiel, B spécifique,
 * D personnalisé, N hors TVA) + catégorie (M morale, P physique, N non-résident)
 * + établissement 000-999, ex. « 1234567A/A/M/000 ».
 * Validation volontairement tolérante (espaces, casse) pour ne jamais bloquer
 * une vente ; le contrôle strict reste du ressort de l'administration fiscale.
 */
public record MatriculeFiscal(String value) {

    private static final Pattern STRICT =
            Pattern.compile("^[0-9]{7,8}[A-Z]?/[ABDPN]/[MPN]/[0-9]{3}$");

    public MatriculeFiscal {
        if (value == null || value.isBlank()) {
            throw new DomainException("Le matricule fiscal est requis");
        }
        value = value.trim().toUpperCase().replaceAll("\\s+", "");
        if (value.length() < 7 || value.length() > 20) {
            throw new DomainException("Matricule fiscal invalide : " + value);
        }
    }

    public static MatriculeFiscal of(String value) {
        return new MatriculeFiscal(value);
    }

    /** Vrai si le format canonique est respecté (contrôle strict). */
    public boolean isCanonical() {
        return STRICT.matcher(value).matches();
    }

    /**
     * Code TVA extrait du matricule (segment après le premier '/').
     * N = hors champ TVA (client sans TVA).
     */
    public String vatCode() {
        int slash = value.indexOf('/');
        if (slash < 0 || slash + 1 >= value.length()) {
            return "A";
        }
        String code = value.substring(slash + 1, slash + 2);
        return "ABDPN".contains(code) ? code : "A";
    }

    /** Vrai si le porteur est hors champ TVA (code N). */
    public boolean isOutsideVatScope() {
        return "N".equals(vatCode());
    }
}
