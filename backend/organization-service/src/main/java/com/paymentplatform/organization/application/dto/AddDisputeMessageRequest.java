package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AddDisputeMessageRequest(
        @NotBlank(message = "Le contenu du message est requis") String content
) {}
