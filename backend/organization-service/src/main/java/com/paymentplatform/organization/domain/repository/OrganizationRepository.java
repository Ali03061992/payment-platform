package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationStatus;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.shared.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    Optional<Organization> findById(OrganizationId id);
    List<Organization> findAll();
    /**
     * B5 : remplace la variante liste complète pour les listes exposées —
     * page 0-based en base, taille déjà bornée par l'appelant.
     */
    PageResult<Organization> findByType(OrganizationType type, int page, int size);
    List<Organization> findByStatus(OrganizationStatus status);
    List<Organization> findByTypeAndStatus(OrganizationType type, OrganizationStatus status);
    boolean existsByName(String name);
    Organization save(Organization organization);
    long countByType(OrganizationType type);
    long countByTypeAndStatus(OrganizationType type, OrganizationStatus status);
}
