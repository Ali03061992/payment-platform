package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.*;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Cas d'usage de consultation des paiements (listes paginées, synthèse
 * par fournisseur et compteurs par statut), avec enrichissement des noms.
 */
@Service
public class ListPaymentsUseCase {

    private final PaymentRepository payments;
    private final PaymentNameResolver nameResolver;
    private final OrganizationValidationClient orgClient;

    public ListPaymentsUseCase(PaymentRepository payments, PaymentNameResolver nameResolver,
                                OrganizationValidationClient orgClient) {
        this.payments = payments;
        this.nameResolver = nameResolver;
        this.orgClient = orgClient;
    }

    /**
     * Liste les paiements émis par une boutique, paginés en mémoire.
     *
     * @param shopId identifiant de la boutique
     * @param page index de page (base 0)
     * @param size taille de page
     * @return page de paiements enrichie
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> execute(UUID shopId, int page, int size) {
        List<Payment> allPayments = payments.findByShopId(shopId);
        return paginate(allPayments, page, size);
    }

    /**
     * Liste les paiements reçus par un fournisseur, paginés en mémoire.
     *
     * @param supplierId identifiant du fournisseur
     * @param page index de page (base 0)
     * @param size taille de page
     * @return page de paiements enrichie
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> executeBySupplier(UUID supplierId, int page, int size) {
        List<Payment> allPayments = payments.findBySupplierId(supplierId);
        return paginate(allPayments, page, size);
    }

    /**
     * Liste tous les paiements (usage administrateur), paginés en mémoire.
     *
     * @param page index de page (base 0)
     * @param size taille de page
     * @return page de paiements enrichie
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> executeAll(int page, int size) {
        List<Payment> allPayments = payments.findAll();
        return paginate(allPayments, page, size);
    }

    private PageResponse<PaymentResponse> paginate(List<Payment> allPayments, int page, int size) {
        int start = Math.min(page * size, allPayments.size());
        int end = Math.min(start + size, allPayments.size());
        List<Payment> pageItems = allPayments.subList(start, end);

        List<PaymentResponse> responses = buildResponses(pageItems);
        int totalPages = (int) Math.ceil((double) allPayments.size() / size);
        return new PageResponse<>(responses, allPayments.size(), totalPages, page);
    }

    private List<PaymentResponse> buildResponses(List<Payment> domainPayments) {
        Set<UUID> orgIds = new LinkedHashSet<>();
        Set<UUID> userIds = new LinkedHashSet<>();

        for (Payment p : domainPayments) {
            orgIds.add(p.shopId());
            orgIds.add(p.supplierId());
            userIds.add(p.createdBy());
            for (var event : p.events()) {
                if (event.userId() != null) userIds.add(event.userId());
            }
        }

        Map<UUID, String> orgNames = new HashMap<>();
        Map<UUID, String> userNames = new HashMap<>();
        for (UUID id : orgIds) {
            orgNames.put(id, orgClient.getOrganizationName(id).orElse("Org " + id));
        }
        for (UUID id : userIds) {
            userNames.put(id, orgClient.getUserName(id).orElse("User " + id));
        }

        PaymentResponse.NameResolver resolver = new PaymentResponse.NameResolver() {
            @Override
            public String resolveOrg(UUID organizationId) {
                return orgNames.getOrDefault(organizationId, "Org " + organizationId);
            }
            @Override
            public String resolveUser(UUID userId) {
                return userNames.getOrDefault(userId, "User " + userId);
            }
        };

        return domainPayments.stream()
                .map(p -> PaymentResponse.from(p, resolver))
                .toList();
    }

    /**
     * B5 : totaux par statut calculés en SQL (COUNT/SUM + GROUP BY) — remplace
     * l'ancien chargement complet ({@code size = Integer.MAX_VALUE}) + boucle Java.
     * Les statuts absents valent zéro ; les autres statuts (ex. CANCELLED) sont
     * ignorés comme dans l'ancien calcul.
     */
    @Transactional(readOnly = true)
    public SupplierPaymentSummaryResponse summarizeBySupplier(UUID supplierId) {
        BigDecimal confirmedTotal = BigDecimal.ZERO;
        long confirmedCount = 0;
        BigDecimal pendingTotal = BigDecimal.ZERO;
        long pendingCount = 0;
        BigDecimal rejectedTotal = BigDecimal.ZERO;
        long rejectedCount = 0;
        for (var summary : payments.summarizeBySupplier(supplierId)) {
            switch (summary.status()) {
                case CONFIRMED -> { confirmedTotal = summary.total(); confirmedCount = summary.count(); }
                case PENDING -> { pendingTotal = summary.total(); pendingCount = summary.count(); }
                case REJECTED -> { rejectedTotal = summary.total(); rejectedCount = summary.count(); }
                default -> {}
            }
        }
        return new SupplierPaymentSummaryResponse(confirmedTotal, confirmedCount,
                pendingTotal, pendingCount, rejectedTotal, rejectedCount);
    }

    /**
     * Calcule les compteurs de paiements par statut via requêtes de comptage.
     *
     * @return statistiques globales (total + détail par statut)
     */
    @Transactional(readOnly = true)
    public PaymentStatsResponse stats() {
        return new PaymentStatsResponse(
                payments.countByStatus(PaymentStatus.PENDING)
                        + payments.countByStatus(PaymentStatus.CONFIRMED)
                        + payments.countByStatus(PaymentStatus.REJECTED)
                        + payments.countByStatus(PaymentStatus.CANCELLED),
                payments.countByStatus(PaymentStatus.PENDING),
                payments.countByStatus(PaymentStatus.CONFIRMED),
                payments.countByStatus(PaymentStatus.REJECTED),
                payments.countByStatus(PaymentStatus.CANCELLED)
        );
    }
}
