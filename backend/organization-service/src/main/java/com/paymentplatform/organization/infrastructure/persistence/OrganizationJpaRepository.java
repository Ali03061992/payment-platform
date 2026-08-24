package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, Long>,
        JpaSpecificationExecutor<OrganizationJpaEntity> {

    Optional<OrganizationJpaEntity> findByName(String name);
    boolean existsByName(String name);
    List<OrganizationJpaEntity> findByType(String type);
    List<OrganizationJpaEntity> findByStatus(String status);
    List<OrganizationJpaEntity> findByTypeAndStatus(String type, String status);

    @Query("SELECT COUNT(o) FROM OrganizationJpaEntity o WHERE o.type = :type")
    long countByType(@Param("type") String type);

    @Query("SELECT COUNT(o) FROM OrganizationJpaEntity o WHERE o.type = :type AND o.status = :status")
    long countByTypeAndStatus(@Param("type") String type, @Param("status") String status);
}
