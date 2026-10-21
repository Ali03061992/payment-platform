package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceJournalJpaRepository extends JpaRepository<InvoiceJournalJpaEntity, UUID> {

    Optional<InvoiceJournalJpaEntity> findByOrderId(UUID orderId);

    java.util.List<InvoiceJournalJpaEntity> findByInvoiceNumber(String invoiceNumber);

    @Query("""
            select e from InvoiceJournalJpaEntity e
            where (:supplierId is null or e.supplierId = :supplierId)
              and (:shopId is null or e.shopId = :shopId)
              and (:year is null or function('year', e.issuedAt) = :year)
            order by e.issuedAt desc
            """)
    Page<InvoiceJournalJpaEntity> search(@Param("supplierId") UUID supplierId,
                                         @Param("shopId") UUID shopId,
                                         @Param("year") Integer year,
                                         Pageable pageable);
}
