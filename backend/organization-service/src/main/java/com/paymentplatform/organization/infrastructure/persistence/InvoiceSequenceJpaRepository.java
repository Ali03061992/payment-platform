package com.paymentplatform.organization.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceJpaRepository extends JpaRepository<InvoiceSequenceJpaEntity, InvoiceSequenceKey> {

    /** Verrou d'écriture sur la ligne compteur : séquence atomique sous concurrence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InvoiceSequenceJpaEntity s where s.supplierId = :supplierId and s.seqYear = :year")
    Optional<InvoiceSequenceJpaEntity> findForUpdate(@Param("supplierId") String supplierId, @Param("year") int year);
}
