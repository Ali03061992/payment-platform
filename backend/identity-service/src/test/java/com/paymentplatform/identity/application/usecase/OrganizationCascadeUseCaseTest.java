package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCascadeUseCaseTest {

    @Mock
    UserRepository users;
    @Mock
    AuditRecorder audit;
    @Mock
    OutboxEventStore outbox;
    @Mock
    EventDeduplicator deduplicator;

    OrganizationCascadeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new OrganizationCascadeUseCase(users, audit, outbox, deduplicator);
    }

    private User member(long id, RoleCode role, UserStatus status) {
        return User.reconstruct(new UserId(id), Username.of("user." + id),
                Email.of("u" + id + "@x.com"), new PasswordHash("h"), "N" + id, "P" + id,
                new PhoneNumber(null), OrganizationId.of(42), status,
                java.util.EnumSet.of(role), 0, java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void supplierDisabled_disablesAllSupplierAdminsAndAgents() {
        when(deduplicator.isProcessed("e1")).thenReturn(false);
        when(users.findByOrganizationId(OrganizationId.of(42))).thenReturn(List.of(
                member(1, RoleCode.SUPPLIER_ADMIN, UserStatus.ACTIVE),
                member(2, RoleCode.SUPPLIER_AGENT, UserStatus.ACTIVE),
                member(3, RoleCode.SHOP_AGENT, UserStatus.ACTIVE)));

        useCase.onOrganizationDisabled("organization.supplier.disabled", 42,
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), "e1");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(u -> u.id().value()).containsExactly(1L, 2L);
        assertThat(captor.getAllValues()).allMatch(u -> u.status() == UserStatus.DISABLED);
        verify(outbox, org.mockito.Mockito.times(2)).append(any(), any());
        verify(deduplicator).markProcessed("e1");
    }

    @Test
    void alreadyDisabledUser_isNotReDisabled() {
        when(deduplicator.isProcessed("e1")).thenReturn(false);
        when(users.findByOrganizationId(OrganizationId.of(42))).thenReturn(List.of(
                member(1, RoleCode.SUPPLIER_ADMIN, UserStatus.DISABLED)));

        useCase.onOrganizationDisabled("organization.supplier.disabled", 42,
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), "e1");

        verify(users, never()).save(any());
        verify(outbox, never()).append(any(), any());
    }

    @Test
    void duplicateEvent_isIgnored() {
        when(deduplicator.isProcessed("e1")).thenReturn(true);

        useCase.onOrganizationDisabled("organization.supplier.disabled", 42,
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), "e1");

        verify(users, never()).findByOrganizationId(any());
        verify(deduplicator, never()).markProcessed("e1");
    }

    @Test
    void shopDisabled_disablesShopAdminsAndAgents() {
        when(deduplicator.isProcessed("e2")).thenReturn(false);
        when(users.findByOrganizationId(OrganizationId.of(42))).thenReturn(List.of(
                member(1, RoleCode.SHOP_ADMIN, UserStatus.ACTIVE),
                member(2, RoleCode.SHOP_AGENT, UserStatus.ACTIVE)));

        useCase.onOrganizationDisabled("organization.shop.disabled", 42,
                List.of(RoleCode.SHOP_ADMIN, RoleCode.SHOP_AGENT), "e2");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(u -> u.status() == UserStatus.DISABLED);
    }
}