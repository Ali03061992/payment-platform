package com.paymentplatform.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByOrganizationId(Long organizationId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}