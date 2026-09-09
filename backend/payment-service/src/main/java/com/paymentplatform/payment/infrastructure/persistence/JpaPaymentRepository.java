package com.paymentplatform.payment.infrastructure.persistence;

import java.util.UUID;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentEvent;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepo.findById(id).map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(id)
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        });
    }

    @Override
    public Optional<Payment> findByReference(String reference) {
        return jpaRepo.findByReference(reference).map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        });
    }

    @Override
    public List<Payment> findAll() {
        return jpaRepo.findAll().stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    @Override
    public List<Payment> findByShopId(UUID shopId) {
        return jpaRepo.findByShopIdOrderByCreatedAtDesc(shopId).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    @Override
    public List<Payment> findBySupplierId(UUID supplierId) {
        return jpaRepo.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return jpaRepo.findByStatusOrderByCreatedAtDesc(status.name()).stream().map(entity -> {
            List<PaymentEvent> events = eventJpaRepo.findByPaymentIdOrderByTimestampAsc(entity.getId())
                    .stream().map(mapper::toEventDomain).toList();
            return mapper.fromFields(entity, events);
        }).toList();
    }

    @Override
    public long countByStatus(PaymentStatus status) {
        return jpaRepo.countByStatus(status.name());
    }

    @Override
    public boolean existsByReference(String reference) {
        return jpaRepo.existsByReference(reference);
    }
}
