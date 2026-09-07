package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrganizationCascadeUseCaseH2Test {

    @Autowired
    private OrganizationCascadeUseCase useCase;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EventDeduplicator deduplicator;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            em.createNativeQuery("DELETE FROM user_roles").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();
            em.createNativeQuery("DELETE FROM processed_events").executeUpdate();
            em.createNativeQuery("DELETE FROM outbox_events").executeUpdate();
            em.createNativeQuery("DELETE FROM audit_logs").executeUpdate();
        });

        User supplierAdmin = User.create(new UserId(0), Username.of("supplier.admin"),
                Email.of("sa@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Admin", "Supplier", new PhoneNumber(null),
                OrganizationId.of(42), RoleCode.SUPPLIER_ADMIN);
        users.save(supplierAdmin);

        User supplierAgent = User.create(new UserId(0), Username.of("supplier.agent"),
                Email.of("agent@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Agent", "Supplier", new PhoneNumber(null),
                OrganizationId.of(42), RoleCode.SUPPLIER_AGENT);
        users.save(supplierAgent);

        User shopAgent = User.create(new UserId(0), Username.of("shop.agent"),
                Email.of("shop@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Agent", "Shop", new PhoneNumber(null),
                OrganizationId.of(42), RoleCode.SHOP_AGENT);
        users.save(shopAgent);
    }

    @Test
    void supplierDisabled_disablesAllSupplierAdminsAndAgents() {
        deduplicator.markProcessed("e1");

        useCase.onOrganizationDisabled("organization.supplier.disabled", 42,
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), "e2");

        List<User> allUsers = users.findByOrganizationId(OrganizationId.of(42));
        List<User> supplierUsers = allUsers.stream()
                .filter(u -> u.roles().contains(RoleCode.SUPPLIER_ADMIN) || u.roles().contains(RoleCode.SUPPLIER_AGENT))
                .toList();

        assertThat(supplierUsers).hasSize(2);
        assertThat(supplierUsers).allMatch(u -> u.status() == UserStatus.DISABLED);
    }

    @Test
    void alreadyDisabledUser_isNotReDisabled() {
        User agent = users.findByUsername(Username.of("supplier.agent")).orElseThrow();
        agent.disable();
        users.save(agent);

        deduplicator.markProcessed("e1");

        useCase.onOrganizationDisabled("organization.supplier.disabled", 42,
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), "e2");

        User remaining = users.findByUsername(Username.of("supplier.agent")).orElseThrow();
        assertThat(remaining.status()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void duplicateEvent_isIgnored() {
        deduplicator.markProcessed("e1");

        useCase.onOrganizationDisabled("organization.supplier.disabled", 42,
                List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), "e1");

        List<User> allUsers = users.findByOrganizationId(OrganizationId.of(42));
        assertThat(allUsers).allMatch(u -> u.status() == UserStatus.ACTIVE);
    }

    @Test
    void shopDisabled_disablesShopAdminsAndAgents() {
        deduplicator.markProcessed("e1");

        useCase.onOrganizationDisabled("organization.shop.disabled", 42,
                List.of(RoleCode.SHOP_ADMIN, RoleCode.SHOP_AGENT), "e2");

        List<User> allUsers = users.findByOrganizationId(OrganizationId.of(42));
        List<User> shopUsers = allUsers.stream()
                .filter(u -> u.roles().contains(RoleCode.SHOP_AGENT))
                .toList();

        assertThat(shopUsers).hasSize(1);
        assertThat(shopUsers).allMatch(u -> u.status() == UserStatus.DISABLED);
    }
}
