package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.WithholdingCertificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithholdingCertificateRepository {
    WithholdingCertificate save(WithholdingCertificate certificate);
    Optional<WithholdingCertificate> findById(UUID id);
    Optional<WithholdingCertificate> findByOrderId(UUID orderId);
    Optional<WithholdingCertificate> findByNumber(String number);
    List<WithholdingCertificate> findByBeneficiaryId(UUID beneficiaryId);
    List<WithholdingCertificate> findByDebtorId(UUID debtorId);
}
