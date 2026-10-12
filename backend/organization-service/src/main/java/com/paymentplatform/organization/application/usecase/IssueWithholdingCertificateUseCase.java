package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.WithholdingCertificate;
import com.paymentplatform.organization.domain.model.WithholdingCertificateStatus;
import com.paymentplatform.organization.domain.model.WithholdingNature;
import com.paymentplatform.organization.domain.model.WithholdingRules;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Émet le certificat de retenue à la source rattaché à la FACTURE DE COMMANDE
 * (art. 55 IRPP/IS — plateforme TEJ, arrêté du 10 mai 2024). Le paiement suit
 * la facture : le certificat naît à la livraison, pas au paiement.
 * Sans assiette (ex. marchandises &lt; 1000 DT TTC) : aucun certificat.
 * Idempotent par commande : un seul certificat, jamais de doublon.
 */
@Service
public class IssueWithholdingCertificateUseCase {

    private final WithholdingCertificateRepository certificates;
    private final OrganizationRepository organizations;

    public IssueWithholdingCertificateUseCase(WithholdingCertificateRepository certificates,
                                             OrganizationRepository organizations) {
        this.certificates = certificates;
        this.organizations = organizations;
    }

    @Transactional
    public Optional<WithholdingCertificate> issueForDeliveredOrder(Order order, String natureStr) {
        Optional<WithholdingCertificate> existing = certificates.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing;
        }

        Organization debtor = organizations.findById(new OrganizationId(order.getShopId()))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + order.getShopId()));
        Organization beneficiary = organizations.findById(new OrganizationId(order.getSupplierId()))
                .orElseThrow(() -> new NotFoundException("Fournisseur non trouvé : " + order.getSupplierId()));

        // Priorité : nature explicite > nature par défaut du fournisseur > auto.
        String declared = (natureStr == null || natureStr.isBlank())
                ? beneficiary.defaultWithholdingNature() : natureStr;
        WithholdingNature nature = WithholdingRules.effectiveNature(
                WithholdingNature.from(declared), beneficiary.matriculeFiscal());
        WithholdingRules.Computation computed =
                WithholdingRules.compute(order.getTotal(), nature);
        if (computed.withholding().signum() <= 0) {
            return Optional.empty();
        }

        LocalDate orderDate = order.getCreatedAt() != null
                ? LocalDate.ofInstant(order.getCreatedAt(), java.time.ZoneId.systemDefault())
                : LocalDate.now();
        WithholdingCertificate certificate = WithholdingCertificate.builder()
                .orderId(order.getId())
                .number(WithholdingRules.certificateNumber())
                .debtorId(debtor.id().value())
                .debtorName(debtor.name().value())
                .debtorMatricule(debtor.matriculeFiscal())
                .debtorAddress(debtor.address())
                .beneficiaryId(beneficiary.id().value())
                .beneficiaryName(beneficiary.name().value())
                .beneficiaryMatricule(beneficiary.matriculeFiscal())
                .beneficiaryAddress(beneficiary.address())
                .nature(nature)
                .grossAmount(order.getTotal())
                .rate(computed.rate())
                .withholdingAmount(computed.withholding())
                .netAmount(computed.net())
                .currency(order.getCurrency())
                .orderReference(order.getReference())
                .orderDate(orderDate)
                .billingPeriod(String.format("%04d-%02d", orderDate.getYear(), orderDate.getMonthValue()))
                .dueDate(WithholdingRules.dueDate(orderDate))
                .status(WithholdingCertificateStatus.READY)
                .build();

        return Optional.of(certificates.save(certificate));
    }

    @Transactional
    public void cancelForOrder(UUID orderId) {
        certificates.findByOrderId(orderId).ifPresent(cert -> {
            if (!cert.status().isTerminal()) {
                cert.cancel();
                certificates.save(cert);
            }
        });
    }
}
