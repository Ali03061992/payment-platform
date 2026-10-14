package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.WithholdingCertificateResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.WithholdingCertificate;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Phase 2 TEJ : lot mensuel à déposer, marquage du dépôt manuel (référence TEJ),
 * rappels d'échéance (fin du mois suivant la facturation — arrêté 10/05/2024).
 */
@Service
public class WithholdingBatchUseCase {

    private final WithholdingCertificateRepository certificates;
    private final OrganizationRepository organizations;

    public WithholdingBatchUseCase(WithholdingCertificateRepository certificates,
                                   OrganizationRepository organizations) {
        this.certificates = certificates;
        this.organizations = organizations;
    }

    private void checkDebtorScope(UUID debtorId, UUID organizationId, boolean systemAdmin) {
        if (!systemAdmin && (organizationId == null || !organizationId.equals(debtorId))) {
            throw new ForbiddenException("Accès refusé aux certificats de ce débiteur");
        }
    }

    /** Certificats non annulés d'un débiteur pour un mois de facturation (fichier à déposer). */
    @Transactional(readOnly = true)
    public List<WithholdingCertificateResponse> exportMonth(UUID debtorId, YearMonth month,
                                                           UUID organizationId, boolean systemAdmin) {
        checkDebtorScope(debtorId, organizationId, systemAdmin);
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        return certificates.findByDebtorId(debtorId).stream()
                .filter(c -> c.status() != com.paymentplatform.organization.domain.model.WithholdingCertificateStatus.CANCELLED)
                .filter(c -> !c.orderDate().isBefore(from) && !c.orderDate().isAfter(to))
                .sorted(Comparator.comparing(WithholdingCertificate::orderDate))
                .map(WithholdingCertificateResponse::from)
                .toList();
    }

    /** Enregistre la référence TEJ après dépôt manuel sur tej.finances.gov.tn. */
    @Transactional
    public WithholdingCertificateResponse markDeposited(UUID orderId, String tejReference,
                                                       UUID organizationId, boolean systemAdmin) {
        WithholdingCertificate cert = certificates.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun certificat de retenue pour la commande : " + orderId));
        checkDebtorScope(cert.debtorId(), organizationId, systemAdmin);
        if (tejReference == null || tejReference.isBlank()) {
            throw new ConflictException("La référence TEJ est obligatoire");
        }
        cert.markDeposited(tejReference.trim());
        return WithholdingCertificateResponse.from(certificates.save(cert));
    }

    /** Certificats à échéance proche (défaut J-7) pour un débiteur, triés par urgence. */
    @Transactional(readOnly = true)
    public List<WithholdingCertificateResponse> dueSoon(UUID debtorId, int days,
                                                       UUID organizationId, boolean systemAdmin) {
        checkDebtorScope(debtorId, organizationId, systemAdmin);
        LocalDate limit = LocalDate.now().plusDays(Math.max(0, days));
        return certificates.findByDebtorId(debtorId).stream()
                .filter(c -> !c.status().isTerminal())
                .filter(c -> !c.dueDate().isAfter(limit))
                .sorted(Comparator.comparing(WithholdingCertificate::dueDate))
                .map(WithholdingCertificateResponse::from)
                .toList();
    }

    /** Synthèse mensuelle d'un débiteur : totaux + ventilation par nature (réconciliation déclaration). */
    @Transactional(readOnly = true)
    public WithholdingMonthlySummary monthlySummary(UUID debtorId, YearMonth month,
                                                   UUID organizationId, boolean systemAdmin) {
        checkDebtorScope(debtorId, organizationId, systemAdmin);
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        var list = certificates.findByDebtorId(debtorId).stream()
                .filter(c -> c.status() != com.paymentplatform.organization.domain.model.WithholdingCertificateStatus.CANCELLED)
                .filter(c -> !c.orderDate().isBefore(from) && !c.orderDate().isAfter(to))
                .toList();
        var byNature = list.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.nature().name(),
                        java.util.stream.Collectors.reducing(
                                java.math.BigDecimal.ZERO,
                                WithholdingCertificate::withholdingAmount,
                                java.math.BigDecimal::add)));
        java.math.BigDecimal grossTotal = list.stream()
                .map(WithholdingCertificate::grossAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal withholdingTotal = byNature.values().stream()
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return new WithholdingMonthlySummary(month.toString(), list.size(), grossTotal, withholdingTotal, byNature);
    }

    /** Identifiant TEJ du débiteur (déposant), si renseigné sur son profil fiscal. */
    @Transactional(readOnly = true)
    public String depositorTejIdentifier(UUID debtorId) {
        return organizations.findById(new OrganizationId(debtorId))
                .map(Organization::tejIdentifier)
                .orElse(null);
    }
}
