package com.paymentplatform.identity.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByOrganizationId(UUID organizationId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}