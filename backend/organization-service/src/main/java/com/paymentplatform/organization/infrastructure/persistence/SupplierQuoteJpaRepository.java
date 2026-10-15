package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierQuoteJpaRepository extends JpaRepository<SupplierQuoteJpaEntity, UUID> {
    List<SupplierQuoteJpaEntity> findByRequestIdOrderByVersionAsc(UUID requestId);
}
