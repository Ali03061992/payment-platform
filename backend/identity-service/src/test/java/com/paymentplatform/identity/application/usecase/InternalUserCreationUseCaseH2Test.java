package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InternalUserCreationUseCaseH2Test {

    @Autowired private InternalUserCreationUseCase useCase;
    @Autowired private UserRepository users;

    @Test
    void createInternalUser_shopAdmin_succeeds() {
        var request = new CreateInternalUserRequest("int.org.admin", "int.admin@org.com",
                "Password123", "Org", "Admin", null, 42L, "SHOP_ADMIN");
        var response = useCase.createInternalUser(request);

        assertThat(response.username()).isEqualTo("int.org.admin");
        assertThat(response.organizationId()).isEqualTo(42L);
        assertThat(response.roles()).containsExactly("SHOP_ADMIN");
    }

    @Test
    void createInternalUser_systemAdmin_noOrg_succeeds() {
        var request = new CreateInternalUserRequest("int.sys.admin", "int.sys@platform.com",
                "Password123", "System", "Admin", null, null, "SYSTEM_ADMIN");
        var response = useCase.createInternalUser(request);

        assertThat(response.organizationId()).isNull();
        assertThat(response.roles()).containsExactly("SYSTEM_ADMIN");
    }

    @Test
    void createInternalUser_systemAdmin_withOrg_throwsConflict() {
        var request = new CreateInternalUserRequest("int.sys.admin2", "int.sys2@platform.com",
                "Password123", "System", "Admin", null, 42L, "SYSTEM_ADMIN");
        assertThatThrownBy(() -> useCase.createInternalUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SYSTEM_ADMIN");
    }

    @Test
    void createInternalUser_nonSystemAdmin_noOrg_throwsConflict() {
        var request = new CreateInternalUserRequest("int.agent", "int.agent@x.com",
                "Password123", "Agent", "Test", null, null, "SHOP_AGENT");
        assertThatThrownBy(() -> useCase.createInternalUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("organisation est requise");
    }

    @Test
    void createInternalUser_duplicateUsername_throwsConflict() {
        var request1 = new CreateInternalUserRequest("int.dup.user", "int.dup1@x.com",
                "Password123", "Dup", "User", null, 42L, "SHOP_AGENT");
        useCase.createInternalUser(request1);

        var request2 = new CreateInternalUserRequest("int.dup.user", "int.dup2@x.com",
                "Password123", "Dup", "User2", null, 42L, "SHOP_AGENT");
        assertThatThrownBy(() -> useCase.createInternalUser(request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Nom d'utilisateur déjà utilisé");
    }

    @Test
    void createInternalUser_duplicateEmail_throwsConflict() {
        var request1 = new CreateInternalUserRequest("int.user1", "int.same@x.com",
                "Password123", "U", "One", null, 42L, "SHOP_AGENT");
        useCase.createInternalUser(request1);

        var request2 = new CreateInternalUserRequest("int.user2", "int.same@x.com",
                "Password123", "U", "Two", null, 42L, "SHOP_AGENT");
        assertThatThrownBy(() -> useCase.createInternalUser(request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Adresse email déjà utilisée");
    }

    @Test
    void createInternalUser_nullPassword_usesDefault() {
        var request = new CreateInternalUserRequest("int.default.pass", "int.dp@x.com",
                null, "Default", "Pass", null, 42L, "SHOP_AGENT");
        var response = useCase.createInternalUser(request);
        assertThat(response.username()).isEqualTo("int.default.pass");
    }

    @Test
    void createInternalUser_blankPassword_usesDefault() {
        var request = new CreateInternalUserRequest("int.blank.pass", "int.bp@x.com",
                "   ", "Blank", "Pass", null, 42L, "SHOP_AGENT");
        var response = useCase.createInternalUser(request);
        assertThat(response.username()).isEqualTo("int.blank.pass");
    }
}
