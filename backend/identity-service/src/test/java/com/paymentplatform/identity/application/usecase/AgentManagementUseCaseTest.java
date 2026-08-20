package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.identity.application.dto.AgentRequest;
import com.paymentplatform.identity.application.port.OrganizationStatus;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.domain.model.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentManagementUseCaseTest {

    @Mock
    UserRepository users;
    @Mock
    OrganizationStatusPort organizationStatus;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuditRecorder audit;
    @Mock
    OutboxEventStore outbox;

    AgentManagementUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AgentManagementUseCase(users, organizationStatus, passwordEncoder, audit, outbox);
    }

    private User supplierAdmin() {
        return User.create(new UserId(1), Username.of("supplier.admin"), Email.of("sa@x.com"),
                PasswordHash.of("h"), "Sam", "Fournier", new PhoneNumber(null), OrganizationId.of(42),
                RoleCode.SUPPLIER_ADMIN);
    }

    private User otherAgent() {
        return User.create(new UserId(99), Username.of("other.agent"), Email.of("other@x.com"),
                PasswordHash.of("h"), "Autre", "Agent", new PhoneNumber(null), OrganizationId.of(7),
                RoleCode.SUPPLIER_AGENT);
    }

    @Test
    void createAgent_success_generatesPasswordAndPublishesEvent() {
        when(users.findById(UserId.of(1))).thenReturn(Optional.of(supplierAdmin()));
        when(organizationStatus.getOrganizationStatus(42))
                .thenReturn(new OrganizationStatus(42, "SUPPLIER", "ACTIVE"));
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.reconstruct(new UserId(200), u.username(), u.email(), u.password(), u.firstName(),
                    u.lastName(), u.phone(), u.organizationId(), u.status(), u.roles(), 0,
                    u.createdAt(), u.updatedAt());
        });

        AgentRequest request = new AgentRequest("Ali", "Ben", "agent@x.com", "+21620000000", "agent.alibaba",
                "SUPPLIER_AGENT", null);

        var created = useCase.createAgent(1, 42, "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request);

        assertThat(created.user().id()).isEqualTo(200);
        assertThat(created.initialPassword()).isNotNull();
        assertThat(created.user().roles()).containsExactly("SUPPLIER_AGENT");
        verify(outbox).append(any(), any());
        verify(audit).record(any(), any(), any(), any(), any());
    }

    @Test
    void createAgent_forAnotherOrganization_throwsForbidden() {
        User actor = supplierAdmin();
        when(users.findById(UserId.of(1))).thenReturn(Optional.of(actor));

        AgentRequest request = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x", "SUPPLIER_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(1, 7, "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void createAgent_forbiddenRole_throwsForbidden() {
        when(users.findById(UserId.of(1))).thenReturn(Optional.of(supplierAdmin()));

        AgentRequest request = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x", "SHOP_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(1, 42, "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createAgent_wrongOrgType_throwsConflict() {
        when(users.findById(UserId.of(1))).thenReturn(Optional.of(supplierAdmin()));
        when(organizationStatus.getOrganizationStatus(42))
                .thenReturn(new OrganizationStatus(42, "SHOP", "ACTIVE"));

        AgentRequest request = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x", "SUPPLIER_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(1, 42, "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createAgent_duplicateUsername_throwsConflict() {
        when(users.findById(UserId.of(1))).thenReturn(Optional.of(supplierAdmin()));
        when(organizationStatus.getOrganizationStatus(42))
                .thenReturn(new OrganizationStatus(42, "SUPPLIER", "ACTIVE"));
        when(users.existsByUsername(any())).thenReturn(true);

        AgentRequest request = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x", "SUPPLIER_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(1, 42, "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void disableAgent_agentOfAnotherOrg_throwsForbidden() {
        when(users.findById(UserId.of(1))).thenReturn(Optional.of(supplierAdmin()));
        when(users.findById(UserId.of(99))).thenReturn(Optional.of(otherAgent()));

        assertThatThrownBy(() -> useCase.disableAgent(1, 42, 99))
                .isInstanceOf(ForbiddenException.class);
        verify(users, never()).save(any());
    }
}