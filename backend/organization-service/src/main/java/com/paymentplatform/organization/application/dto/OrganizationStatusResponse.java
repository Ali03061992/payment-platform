package com.paymentplatform.organization.application.dto;

import java.util.UUID;

public record OrganizationStatusResponse(
        UUID id,
        String name,
        String type,
        String status
) {}
