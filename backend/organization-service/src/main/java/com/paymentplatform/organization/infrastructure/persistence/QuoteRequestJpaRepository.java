package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRequestJpaRepository extends JpaRepository<QuoteRequestJpaEntity, UUID> {
    Optional<QuoteRequestJpaEntity> findByReference(String reference);
    List<QuoteRequestJpaEntity> findByShopIdOrderByCreatedAtDesc(UUID shopId);
    List<QuoteRequestJpaEntity> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
}
