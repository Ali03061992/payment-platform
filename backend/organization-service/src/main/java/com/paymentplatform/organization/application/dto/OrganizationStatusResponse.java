package com.paymentplatform.organization.application.dto;

public record OrganizationStatusResponse(
        long id,
        String type,
        String status
) {}
