package com.paymentplatform.payment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentRequest(
        @NotBlank(message = "Le motif de rejet est obligatoire")
        @Size(max = 500, message = "Le motif de rejet ne peut pas dépasser 500 caractères")
        String rejectionReason
) {}
