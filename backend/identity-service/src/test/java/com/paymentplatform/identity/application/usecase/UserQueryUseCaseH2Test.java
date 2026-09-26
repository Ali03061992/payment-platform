package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
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
 * Tests de UserQueryUseCaseH2Test.
 * Perimetre : cas d'usage/service UserQueryUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserQueryUseCaseH2Test {

    @Autowired private UserQueryUseCase query;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final UUID UNIQUE_ORG = UUID.fromString("00000000-0000-0000-0000-000000000088");

    private UUID adminId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        User admin = User.create(new UserId(null), Username.of("uq3sysadmin"),
                Email.of("uq3admin@platform.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "System", "Admin", new PhoneNumber(null), null, RoleCode.SYSTEM_ADMIN);
        users.save(admin);
        adminId = users.findByUsername(Username.of("uq3sysadmin")).orElseThrow().id().value();

        User agent = User.create(new UserId(null), Username.of("uq3supplier.agent"),
                Email.of("uq3agent@supplier.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Supplier", "Agent", new PhoneNumber(null),
                OrganizationId.of(UNIQUE_ORG), RoleCode.SUPPLIER_AGENT);
        users.save(agent);
        agentId = users.findByUsername(Username.of("uq3supplier.agent")).orElseThrow().id().value();
    }

    @Test
    void findById_systemAdmin_canSeeAnyone() {
        var response = query.findById(adminId, agentId,
                List.of("SYSTEM_ADMIN"), null);
        assertThat(response.username()).isEqualTo("uq3supplier.agent");
    }

    @Test
    void findById_sameOrg_canSee() {
        User shopAdmin = User.create(new UserId(null), Username.of("uq3shop.admin"),
                Email.of("uq3sa@shop.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Shop", "Admin", new PhoneNumber(null),
                OrganizationId.of(UNIQUE_ORG), RoleCode.SHOP_ADMIN);
        users.save(shopAdmin);
        UUID shopAdminId = users.findByUsername(Username.of("uq3shop.admin")).orElseThrow().id().value();

        var response = query.findById(shopAdminId, agentId,
                List.of("SHOP_ADMIN"), UNIQUE_ORG);
        assertThat(response.username()).isEqualTo("uq3supplier.agent");
    }

    @Test
    void findById_differentOrg_throwsForbidden() {
        User otherAgent = User.create(new UserId(null), Username.of("uq3other.agent"),
                Email.of("uq3other@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Other", "Agent", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000099999")), RoleCode.SUPPLIER_AGENT);
        users.save(otherAgent);
        UUID otherId = users.findByUsername(Username.of("uq3other.agent")).orElseThrow().id().value();

        assertThatThrownBy(() -> query.findById(agentId, otherId,
                List.of("SUPPLIER_AGENT"), UNIQUE_ORG))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findById_notFound_throws() {
        assertThatThrownBy(() -> query.findById(adminId, UUID.fromString("00000000-0000-0000-0000-000000099999"),
                List.of("SYSTEM_ADMIN"), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_systemAdmin_canListAll() {
        var result = query.list(adminId, List.of("SYSTEM_ADMIN"), null, null, null, null, 0, 20);
        assertThat(result.items()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void list_systemAdmin_filterByOrg() {
        var result = query.list(adminId, List.of("SYSTEM_ADMIN"), null, UNIQUE_ORG, null, null, 0, 20);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).organizationId()).isEqualTo(UNIQUE_ORG);
    }

    @Test
    void list_systemAdmin_filterByRole() {
        var result = query.list(adminId, List.of("SYSTEM_ADMIN"), null, UNIQUE_ORG, "SUPPLIER_AGENT", null, 0, 20);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void list_pagination_secondPage() {
        var page0 = query.list(adminId, List.of("SYSTEM_ADMIN"), null, null, null, null, 0, 1);
        var page1 = query.list(adminId, List.of("SYSTEM_ADMIN"), null, null, null, null, 1, 1);
        assertThat(page0.items()).hasSize(1);
        assertThat(page1.items()).hasSize(1);
        assertThat(page1.totalElements()).isEqualTo(page0.totalElements());
        assertThat(page1.number()).isEqualTo(1);
    }

    @Test
    void list_sizeIsCappedAt100() {
        for (int i = 0; i < 105; i++) {
            User u = User.create(new UserId(null), Username.of("uq3bulk" + i),
                    Email.of("uq3bulk" + i + "@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                    "Bulk", "User", new PhoneNumber(null),
                    OrganizationId.of(UNIQUE_ORG), RoleCode.SHOP_AGENT);
            users.save(u);
        }
        var result = query.list(adminId, List.of("SYSTEM_ADMIN"), null, UNIQUE_ORG, null, null, 0, 500);
        assertThat(result.items()).hasSizeLessThanOrEqualTo(100);
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(105);
    }

    @Test
    void list_nonAdmin_sameOrg() {
        User shopAgent = User.create(new UserId(null), Username.of("uq3shop.ag"),
                Email.of("uq3sa2@shop.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Shop", "Agent", new PhoneNumber(null),
                OrganizationId.of(UNIQUE_ORG), RoleCode.SHOP_AGENT);
        users.save(shopAgent);
        UUID shopAgentId = users.findByUsername(Username.of("uq3shop.ag")).orElseThrow().id().value();

        var result = query.list(shopAgentId, List.of("SHOP_AGENT"), UNIQUE_ORG, null, null, null, 0, 20);
        assertThat(result.items()).isNotEmpty();
    }

    @Test
    void list_nonAdmin_nullOrg_throwsForbidden() {
        assertThatThrownBy(() -> query.list(UUID.fromString("00000000-0000-0000-0000-000000000001"), List.of("SHOP_AGENT"), null, null, null, null, 0, 20))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findByIdInternal_works() {
        var response = query.findByIdInternal(agentId);
        assertThat(response.username()).isEqualTo("uq3supplier.agent");
    }
}
