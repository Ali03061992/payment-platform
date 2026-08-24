package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationStatus;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    Optional<Organization> findById(OrganizationId id);
    List<Organization> findAll();
    List<Organization> findByType(OrganizationType type);
    List<Organization> findByStatus(OrganizationStatus status);
    List<Organization> findByTypeAndStatus(OrganizationType type, OrganizationStatus status);
    boolean existsByName(String name);
    Organization save(Organization organization);
    long countByType(OrganizationType type);
    long countByTypeAndStatus(OrganizationType type, OrganizationStatus status);
}
