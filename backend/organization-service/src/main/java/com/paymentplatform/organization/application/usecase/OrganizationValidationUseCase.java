package com.paymentplatform.organization.application.usecase;

import java.util.UUID;

import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.organization.application.dto.OrganizationStatusResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationValidationUseCase {

    private final OrganizationRepository organizations;

    public OrganizationValidationUseCase(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public OrganizationStatusResponse validate(UUID organizationId) {
        Organization org = organizations.findById(OrganizationId.of(organizationId))
                .orElseThrow(() -> new NotFoundException("Organisation non trouvée : " + organizationId));
        return new OrganizationStatusResponse(org.id().value(), org.name().value(), org.type().name(), org.status().name());
    }
}
