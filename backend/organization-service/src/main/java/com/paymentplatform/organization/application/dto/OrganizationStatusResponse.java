package com.paymentplatform.organization.application.dto;

public record OrganizationStatusResponse(
        long id,
        String name,
        String type,
        String status
) {}
