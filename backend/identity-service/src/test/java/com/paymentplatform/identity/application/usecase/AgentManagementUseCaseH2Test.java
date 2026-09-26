package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.AgentRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de AgentManagementUseCaseH2Test.
 * Perimetre : cas d'usage/service AgentManagementUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentManagementUseCaseH2Test {

    @Autowired
    private AgentManagementUseCase useCase;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User supplierAdmin = User.create(new UserId(null), Username.of("supplier.admin"),
                Email.of("sa@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Sam", "Fournier", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000042")), RoleCode.SUPPLIER_ADMIN);
        users.save(supplierAdmin);
    }

    @Test
    void createAgent_success_generatesPasswordAndPublishesEvent() {
        User actor = users.findByUsername(Username.of("supplier.admin")).orElseThrow();

        AgentRequest request = new AgentRequest("Ali", "Ben", "agent@x.com", "+21620000000",
                "agent.alibaba", "SUPPLIER_AGENT", null);

        var created = useCase.createAgent(actor.id().value(), UUID.fromString("00000000-0000-0000-0000-000000000042"), "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request);

        assertThat(created.user().id()).isNotNull();
        assertThat(created.initialPassword()).isNotNull();
        assertThat(created.user().roles()).containsExactly("SUPPLIER_AGENT");
    }

    @Test
    void createAgent_forAnotherOrganization_throwsForbidden() {
        User actor = users.findByUsername(Username.of("supplier.admin")).orElseThrow();

        AgentRequest request = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x",
                "SUPPLIER_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(actor.id().value(), UUID.fromString("00000000-0000-0000-0000-000000000007"), "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createAgent_forbiddenRole_throwsForbidden() {
        User actor = users.findByUsername(Username.of("supplier.admin")).orElseThrow();

        AgentRequest request = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x",
                "SHOP_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(actor.id().value(), UUID.fromString("00000000-0000-0000-0000-000000000042"), "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createAgent_duplicateUsername_throwsConflict() {
        User actor = users.findByUsername(Username.of("supplier.admin")).orElseThrow();

        AgentRequest request1 = new AgentRequest("Ali", "Ben", "a@x.com", null, "agent.x",
                "SUPPLIER_AGENT", null);
        useCase.createAgent(actor.id().value(), UUID.fromString("00000000-0000-0000-0000-000000000042"), "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request1);

        AgentRequest request2 = new AgentRequest("Ali", "Ben", "b@x.com", null, "agent.x",
                "SUPPLIER_AGENT", null);

        assertThatThrownBy(() -> useCase.createAgent(actor.id().value(), UUID.fromString("00000000-0000-0000-0000-000000000042"), "SUPPLIER",
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Nom d'utilisateur déjà utilisé");
    }

    @Test
    void disableAgent_agentOfAnotherOrg_throwsForbidden() {
        User actor = users.findByUsername(Username.of("supplier.admin")).orElseThrow();

        User otherAgent = User.create(new UserId(null), Username.of("other.agent"),
                Email.of("other@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Autre", "Agent", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000007")), RoleCode.SUPPLIER_AGENT);
        User savedOther = users.save(otherAgent);

        assertThatThrownBy(() -> useCase.disableAgent(actor.id().value(), UUID.fromString("00000000-0000-0000-0000-000000000042"), savedOther.id().value()))
                .isInstanceOf(ForbiddenException.class);
    }
}
