package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.WithholdingCertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithholdingCertificateJpaRepository extends JpaRepository<WithholdingCertificateJpaEntity, UUID> {
    Optional<WithholdingCertificateJpaEntity> findByOrderId(UUID orderId);
    Optional<WithholdingCertificateJpaEntity> findByNumber(String number);
    List<WithholdingCertificateJpaEntity> findByBeneficiaryId(UUID beneficiaryId);
    List<WithholdingCertificateJpaEntity> findByDebtorId(UUID debtorId);
    List<WithholdingCertificateJpaEntity> findByDueDateLessThanEqualAndStatusNot(
            LocalDate limit, WithholdingCertificateStatus status);
}
