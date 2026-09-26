package com.paymentplatform.identity.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.application.usecase.OrganizationCascadeUseCase;
import com.paymentplatform.shared.domain.event.OrganizationEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Tests de OrganizationEventConsumerTest.
 * Perimetre : infrastructure OrganizationEventConsumer (messagerie/config/persistance).
 * Moyens : contexte SpringBootTest, profil "test" (H2), mocks Mockito.
 */

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
                {"eventType":"%s","eventId":"evt-1","organizationId":"00000000-0000-0000-0000-000000000042"}
                """.formatted(OrganizationEvents.SupplierDisabledEvent.EVENT_TYPE);

        consumer.onOrganizationEvent(payload);

        verify(cascade).onOrganizationDisabled(eq(OrganizationEvents.SupplierDisabledEvent.EVENT_TYPE),
                eq(UUID.fromString("00000000-0000-0000-0000-000000000042")), anyList(), eq("evt-1"));
    }

    @Test
    void onOrganizationEvent_shopDisabled_callsCascade() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        String payload = """
                {"eventType":"%s","eventId":"evt-2","organizationId":"00000000-0000-0000-0000-000000000007"}
                """.formatted(OrganizationEvents.ShopDisabledEvent.EVENT_TYPE);

        consumer.onOrganizationEvent(payload);

        verify(cascade).onOrganizationDisabled(eq(OrganizationEvents.ShopDisabledEvent.EVENT_TYPE),
                eq(UUID.fromString("00000000-0000-0000-0000-000000000007")), anyList(), eq("evt-2"));
    }

    @Test
    void onOrganizationEvent_unknownType_doesNothing() {
        OrganizationCascadeUseCase cascade = mock(OrganizationCascadeUseCase.class);
        OrganizationEventConsumer consumer = new OrganizationEventConsumer(cascade, objectMapper);

        String payload = """
                {"eventType":"organization.unknown","eventId":"evt-3","organizationId":"00000000-0000-0000-0000-000000000001"}
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
