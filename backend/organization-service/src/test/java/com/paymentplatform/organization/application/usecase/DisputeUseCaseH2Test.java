package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DisputeUseCaseH2Test {

    @Autowired private DisputeRepository disputes;
    @Autowired private OrderRepository orders;
    @Autowired private ResolveDisputeUseCase resolveDispute;

    @Test
    void resolveOpenDispute_advancesThroughInProgress() {
        UUID supplierId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Order order = orders.save(Order.create(supplierId, shopId, actorId, "SHOP", "SHOP", false, "TND", null));
        Dispute dispute = disputes.save(Dispute.create(order.getId(), shopId, supplierId, actorId, "Colis endommagé"));

        DisputeResponse response = resolveDispute.execute(dispute.getId(), actorId, "RESOLVED");

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(disputes.findById(dispute.getId()).orElseThrow().getStatus()).isEqualTo("RESOLVED");
    }
}
