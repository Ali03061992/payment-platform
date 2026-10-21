package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(min = 2, max = 100) String name,
        String matriculeFiscal,
        String patente,
        String address,
        String fiscalRegime,
        String suspensionAttestationNumber,
        java.time.LocalDate suspensionAttestationDate,
        java.time.LocalDate suspensionExpiryDate,
        String tejIdentifier,
        String defaultWithholdingNature,
        Boolean teifEnabled,
        Boolean tejEnabled
) {
    public UpdateOrganizationRequest(String name) {
        this(name, null, null, null, null, null, null, null, null, null, null, null);
    }

    public UpdateOrganizationRequest(String name, String matriculeFiscal, String patente, String address,
                                     String fiscalRegime, String suspensionAttestationNumber,
                                     java.time.LocalDate suspensionAttestationDate,
                                     java.time.LocalDate suspensionExpiryDate,
                                     String tejIdentifier, String defaultWithholdingNature) {
        this(name, matriculeFiscal, patente, address, fiscalRegime, suspensionAttestationNumber,
                suspensionAttestationDate, suspensionExpiryDate, tejIdentifier, defaultWithholdingNature,
                null, null);
    }
}
