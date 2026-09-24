package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderCommentRequest(
        @NotBlank(message = "Le contenu du commentaire est requis")
        @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
        String content
) {}
