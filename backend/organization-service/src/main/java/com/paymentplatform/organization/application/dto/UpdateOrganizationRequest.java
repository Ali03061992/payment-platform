package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(min = 2, max = 100) String name
) {}
