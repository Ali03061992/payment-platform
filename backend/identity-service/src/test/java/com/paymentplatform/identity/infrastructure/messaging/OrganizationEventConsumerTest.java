package com.paymentplatform.identity.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.application.usecase.OrganizationCascadeUseCase;
import com.paymentplatform.shared.domain.event.OrganizationEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationEventConsumerTest {

    @Autowired private ObjectMapper objectMapper;

    @Test
    void onOrganizationEvent_supplierDisabled_callsCascade() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        String payload = """
                {"eventType":"%s","eventId":"evt-1","organizationId":42}
                """.formatted(OrganizationEvents.SupplierDisabledEvent.EVENT_TYPE);

        consumer.onOrganizationEvent(payload);

        verify(cascade).onOrganizationDisabled(eq(OrganizationEvents.SupplierDisabledEvent.EVENT_TYPE),
                eq(42L), anyList(), eq("evt-1"));
    }

    @Test
    void onOrganizationEvent_shopDisabled_callsCascade() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        String payload = """
                {"eventType":"%s","eventId":"evt-2","organizationId":7}
                """.formatted(OrganizationEvents.ShopDisabledEvent.EVENT_TYPE);

        consumer.onOrganizationEvent(payload);

        verify(cascade).onOrganizationDisabled(eq(OrganizationEvents.ShopDisabledEvent.EVENT_TYPE),
                eq(7L), anyList(), eq("evt-2"));
    }

    @Test
    void onOrganizationEvent_unknownType_doesNothing() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        String payload = """
                {"eventType":"organization.unknown","eventId":"evt-3","organizationId":1}
                """;

        consumer.onOrganizationEvent(payload);
        verifyNoInteractions(cascade);
    }

    @Test
    void onOrganizationEvent_missingFields_doesNothing() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        String payload = """
                {"eventId":"evt-4"}
                """;

        consumer.onOrganizationEvent(payload);
        verifyNoInteractions(cascade);
    }

    @Test
    void onOrganizationEvent_invalidPayload_throws() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        assertThatThrownBy(() -> consumer.onOrganizationEvent("not json"))
                .isInstanceOf(IllegalStateException.class);
    }
}
