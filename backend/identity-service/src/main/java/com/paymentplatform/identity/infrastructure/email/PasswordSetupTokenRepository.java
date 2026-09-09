package com.paymentplatform.identity.infrastructure.email;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, UUID> {

    Optional<PasswordSetupToken> findByTokenAndUsedFalse(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordSetupToken t WHERE t.userId = :userId")
    void deleteByUserId(UUID userId);
}
