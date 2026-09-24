package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID>,
        JpaSpecificationExecutor<OrganizationJpaEntity> {

    Optional<OrganizationJpaEntity> findByName(String name);
    boolean existsByName(String name);
    List<OrganizationJpaEntity> findByType(String type);

    /** B5 : variante paginée en base pour les listes exposées. */
    Page<OrganizationJpaEntity> findByType(String type, Pageable pageable);
    List<OrganizationJpaEntity> findByStatus(String status);
    List<OrganizationJpaEntity> findByTypeAndStatus(String type, String status);

    @Query("SELECT COUNT(o) FROM OrganizationJpaEntity o WHERE o.type = :type")
    long countByType(@Param("type") String type);

    @Query("SELECT COUNT(o) FROM OrganizationJpaEntity o WHERE o.type = :type AND o.status = :status")
    long countByTypeAndStatus(@Param("type") String type, @Param("status") String status);
}
