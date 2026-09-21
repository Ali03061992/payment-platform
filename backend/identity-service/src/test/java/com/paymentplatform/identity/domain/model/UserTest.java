package com.paymentplatform.identity.domain.model;

import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private User sample() {
        return User.create(new UserId(null), Username.of("agent.one"),
                Email.of("agent.one@example.com"), PasswordHash.of("hash"),
                "Ali", "Ben Ammar", new PhoneNumber("+21620123456"),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000007")), RoleCode.SUPPLIER_AGENT);
    }

    @Test
    void create_assignsActiveStatusAndRole() {
        User user = sample();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.roles()).containsExactly(RoleCode.SUPPLIER_AGENT);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void disable_thenActivate_isIdempotent() {
        User user = sample();
        user.disable();
        user.disable();
        assertThat(user.status()).isEqualTo(UserStatus.DISABLED);
        user.activate();
        user.activate();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void invalidUsername_throwsDomainException() {
        assertThatThrownBy(() -> Username.of("ab"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void invalidEmail_throwsDomainException() {
        assertThatThrownBy(() -> Email.of("not-an-email"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void assertCanManageOrganization_allowsOwnOrgOnly() {
        User user = sample();
        user.assertCanManageOrganization(OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000007")));
        assertThatThrownBy(() -> user.assertCanManageOrganization(OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000008"))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void systemAdmin_cannotManageAnyOrganization() {
        User admin = User.create(new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")), Username.of("system.admin"),
                Email.of("admin@platform.local"), PasswordHash.of("hash"), "System", "Admin",
                new PhoneNumber(null), null, RoleCode.SYSTEM_ADMIN);
        assertThatThrownBy(() -> admin.assertCanManageOrganization(OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
                .isInstanceOf(ConflictException.class);
    }
}