package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Renommage d'une passkey enregistrée. */
public record WebAuthnRenameRequest(
        @NotBlank(message = "le nom de l'appareil est obligatoire")
        @Size(max = 100, message = "le nom de l'appareil dépasse 100 caractères")
        String label) {
}
