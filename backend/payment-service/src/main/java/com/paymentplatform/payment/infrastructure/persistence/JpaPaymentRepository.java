package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentEvent;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.model.PaymentStatusSummary;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptateur JPA du dépôt de paiements : persiste l'agrégat et rejoue
 * ses événements via les entités JPA dédiées.
 */
@Component
public class JpaPaymentRepository implements PaymentRepository {

    private final PaymentJpaRepository jpaRepo;
    private final PaymentEventJpaRepository eventJpaRepo;
    private final PaymentMapper mapper;

    public JpaPaymentRepository(PaymentJpaRepository jpaRepo,
                                PaymentEventJpaRepository eventJpaRepo,
                                PaymentMapper mapper) {
        this.jpaRepo = jpaRepo;
        this.eventJpaRepo = eventJpaRepo;
        this.mapper = mapper;
    }

    /**
     * Sauvegarde un paiement puis persiste ses nouveaux événements de domaine.
     *
     * @param payment agrégat à sauvegarder
     * @return agrégat rechargé avec l'historique complet
     */
    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = mapper.toJpa(payment);
        PaymentJpaEntity saved = jpaRepo.save(entity);

        List<PaymentEvent> pendingEvents = payment.events().stream()
                .filter(e -> e.id() == null)
                .toList();
        for (PaymentEvent event : pendingEvents) {
            eventJpaRepo.save(mapper.toEventJpa(event, saved.getId()));
        }

        List<PaymentEvent> allEvents = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(saved.getId())
                .stream().map(mapper::toEventDomain).toList();

        return mapper.fromFields(saved, allEvents);
    }

    /**
     * Recherche un paiement par identifiant avec son historique d'événements.
     *
     * @param id identifiant du paiement
     * @return paiement si trouvé
     */
    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepo.findById(id).map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(id)
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        });
    }

    /**
     * Recherche un paiement par référence métier avec son historique.
     *
     * @param reference référence du paiement
     * @return paiement si trouvé
     */
    @Override
    public Optional<Payment> findByReference(String reference) {
        return jpaRepo.findByReference(reference).map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        });
    }

    /**
     * Recherche un paiement par clé d'idempotence (rejeu sans doublon).
     *
     * @param idempotencyKey clé anti-doublon
     * @return paiement si trouvé
     */
    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepo.findByIdempotencyKey(idempotencyKey).map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        });
    }

    /**
     * Liste tous les paiements avec leur historique d'événements.
     *
     * @return ensemble des paiements
     */
    @Override
    public List<Payment> findAll() {
        return jpaRepo.findAll().stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    /**
     * Liste les paiements d'une boutique, triés par date décroissante.
     *
     * @param shopId identifiant de la boutique
     * @return paiements de la boutique
     */
    @Override
    public List<Payment> findByShopId(UUID shopId) {
        return jpaRepo.findByShopIdOrderByCreatedAtDesc(shopId).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    /**
     * Liste les paiements d'un fournisseur, triés par date décroissante.
     *
     * @param supplierId identifiant du fournisseur
     * @return paiements du fournisseur
     */
    @Override
    public List<Payment> findBySupplierId(UUID supplierId) {
        return jpaRepo.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    /**
     * Liste les paiements dans un statut donné, triés par date décroissante.
     *
     * @param status statut filtré
     * @return paiements du statut
     */
    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return jpaRepo.findByStatusOrderByCreatedAtDesc(status).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    /**
     * Compte les paiements dans un statut donné.
     *
     * @param status statut compté
     * @return nombre de paiements
     */
    @Override
    public long countByStatus(PaymentStatus status) {
        return jpaRepo.countByStatus(status);
    }

    /**
     * Vérifie l'existence d'un paiement par référence métier.
     *
     * @param reference référence testée
     * @return vrai si la référence existe déjà
     */
    @Override
    public boolean existsByReference(String reference) {
        return jpaRepo.existsByReference(reference);
    }

    /**
     * Calcule les totaux par statut pour un fournisseur (agrégats SQL).
     *
     * @param supplierId fournisseur concerné
     * @return résumés par statut (total et compteur)
     */
    @Override
    public List<PaymentStatusSummary> summarizeBySupplier(UUID supplierId) {
        return jpaRepo.summarizeBySupplier(supplierId).stream()
                .map(a -> new PaymentStatusSummary(a.getStatus(), a.getCnt(),
                        a.getTotal() == null ? java.math.BigDecimal.ZERO : a.getTotal()))
                .toList();
    }

    /**
     * Liste les paiements échus à une date donnée (tous périmètres).
     *
     * @param today date de référence de l'échéance
     * @return paiements en retard
     */
    @Override
    public List<Payment> findOverdue(LocalDate today) {
        return jpaRepo.findOverduePayments(today).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    /**
     * Liste les paiements échus d'un fournisseur à une date donnée.
     *
     * @param today date de référence de l'échéance
     * @param supplierId fournisseur concerné
     * @return paiements en retard du fournisseur
     */
    @Override
    public List<Payment> findOverdueBySupplier(LocalDate today, UUID supplierId) {
        return jpaRepo.findOverduePaymentsBySupplier(today, supplierId).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    /**
     * Liste les paiements échus d'une boutique à une date donnée.
     *
     * @param today date de référence de l'échéance
     * @param shopId boutique concernée
     * @return paiements en retard de la boutique
     */
    @Override
    public List<Payment> findOverdueByShop(LocalDate today, UUID shopId) {
        return jpaRepo.findOverduePaymentsByShop(today, shopId).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }
}
