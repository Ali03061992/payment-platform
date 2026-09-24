package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.shared.domain.model.RoleCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByOrganizationId(UUID organizationId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * B5 : recherche filtrée + paginée en base (aucun chargement complet).
     * Tous les filtres sont nullables. Le rôle est porté par la table
     * secondaire user_roles (ElementCollection).
     */
    @Query("SELECT DISTINCT u FROM UserJpaEntity u LEFT JOIN u.roles r"
            + " WHERE (:organizationId IS NULL OR u.organizationId = :organizationId)"
            + " AND (:status IS NULL OR u.status = :status)"
            + " AND (:role IS NULL OR r = :role)")
    Page<UserJpaEntity> search(@Param("organizationId") UUID organizationId,
                               @Param("status") String status,
                               @Param("role") RoleCode role,
                               Pageable pageable);
}