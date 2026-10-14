package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.domain.model.WithholdingCertificate;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rappel quotidien J-7 des certificats de retenue à échéance (fin du mois suivant
 * la facturation — arrêté 10/05/2024). Publie un événement par certificat pour le
 * service de notification (même contrat que les alertes stock).
 */
@Component
public class WithholdingDueScheduler {

    private static final Logger log = LoggerFactory.getLogger(WithholdingDueScheduler.class);

    private final WithholdingCertificateRepository certificates;
    private final OutboxEventStore outboxEventStore;

    public WithholdingDueScheduler(WithholdingCertificateRepository certificates,
                                   OutboxEventStore outboxEventStore) {
        this.certificates = certificates;
        this.outboxEventStore = outboxEventStore;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void remindDueCertificates() {
        LocalDate limit = LocalDate.now().plusDays(7);
        List<WithholdingCertificate> due = certificates.findDueBefore(limit);
        log.info("Found {} withholding certificates due within 7 days", due.size());
        for (WithholdingCertificate c : due) {
            OrderEvents.WithholdingDueReminderEvent event = new OrderEvents.WithholdingDueReminderEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    c.id(),
                    c.number(),
                    c.debtorId(),
                    c.orderId(),
                    c.orderReference(),
                    c.dueDate().toString(),
                    c.withholdingAmount().toPlainString());
            outboxEventStore.append(event, String.valueOf(c.id()));
            log.info("Published withholding due reminder for certificate {} (due {})", c.number(), c.dueDate());
        }
    }
}
