package com.paymentplatform.identity.application.dto;

/** Résultat de création d'agent : inclut le mot de passe initial généré (une seule fois). */
public record AgentCreatedResponse(UserResponse user, String initialPassword) {
}