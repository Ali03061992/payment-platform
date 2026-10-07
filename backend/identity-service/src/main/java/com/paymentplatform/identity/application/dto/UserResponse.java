package com.paymentplatform.identity.application.dto;

import com.paymentplatform.identity.domain.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String username, String email, String firstName, String lastName,
                           String phone, UUID organizationId, List<String> roles, String status,
                           String preferredLang, String accentColor1, String accentColor2, boolean tourSeen,
                           Instant createdAt, Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().value(),
                user.username().value(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.phone() == null ? null : user.phone().value(),
                user.organizationId() == null ? null : user.organizationId().value(),
                user.roles().stream().map(Enum::name).sorted().toList(),
                user.status().name(),
                user.preferredLang(),
                user.accentColor1(),
                user.accentColor2(),
                user.tourSeen(),
                user.createdAt(),
                user.updatedAt());
    }
}