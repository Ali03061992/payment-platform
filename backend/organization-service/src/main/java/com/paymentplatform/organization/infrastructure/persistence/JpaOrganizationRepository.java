package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationName;
import com.paymentplatform.organization.domain.valueobject.OrganizationStatus;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class JpaOrganizationRepository implements OrganizationRepository {

    private final OrganizationJpaRepository jpa;

    public JpaOrganizationRepository(OrganizationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> findById(OrganizationId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Organization> findByType(OrganizationType type, int page, int size) {
        Page<OrganizationJpaEntity> result = jpa.findByType(type.name(), PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> findByStatus(OrganizationStatus status) {
        return jpa.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> findByTypeAndStatus(OrganizationType type, OrganizationStatus status) {
        return jpa.findByTypeAndStatus(type.name(), status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    @Transactional
    public Organization save(Organization organization) {
        OrganizationJpaEntity entity = toEntity(organization);
        OrganizationJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByType(OrganizationType type) {
        return jpa.countByType(type.name());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTypeAndStatus(OrganizationType type, OrganizationStatus status) {
        return jpa.countByTypeAndStatus(type.name(), status.name());
    }

    private OrganizationJpaEntity toEntity(Organization org) {
        OrganizationJpaEntity e = new OrganizationJpaEntity();
        if (org.id() != null && org.id() != null && org.id().value() != null) {
            e.setId(org.id().value());
        }
        e.setName(org.name().value());
        e.setType(org.type().name());
        e.setStatus(org.status().name());
        e.setVersion(org.version());
        e.setCreatedAt(org.createdAt());
        e.setUpdatedAt(org.updatedAt());
        return e;
    }

    private Organization toDomain(OrganizationJpaEntity e) {
        return Organization.reconstruct(
                OrganizationId.of(e.getId()),
                OrganizationName.of(e.getName()),
                OrganizationType.valueOf(e.getType()),
                OrganizationStatus.valueOf(e.getStatus()),
                e.getVersion() == null ? 0 : e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
