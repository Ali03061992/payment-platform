package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Recherche globale par rôle pour les appels internes (ex. notif admins). */
@ExtendWith(MockitoExtension.class)
class UserQueryRoleTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private UserQueryUseCase useCase;

    private static User admin(String username) {
        return User.create(
                new UserId(UUID.randomUUID()),
                Username.of(username),
                Email.of(username + "@e2e.test"),
                PasswordHash.of("hash"),
                "Admin", "Test", null, null,
                RoleCode.SYSTEM_ADMIN);
    }

    @Test
    void listByRoleInternal_delegatesToRepository() {
        User adm = admin("sys.admin");
        when(users.findByRole(RoleCode.SYSTEM_ADMIN)).thenReturn(List.of(adm));

        List<UserResponse> result = useCase.listByRoleInternal("SYSTEM_ADMIN");

        verify(users).findByRole(RoleCode.SYSTEM_ADMIN);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("sys.admin");
        assertThat(result.get(0).roles()).contains("SYSTEM_ADMIN");
    }

    @Test
    void listByRoleInternal_emptyWhenNobody() {
        when(users.findByRole(RoleCode.SYSTEM_ADMIN)).thenReturn(List.of());

        assertThat(useCase.listByRoleInternal("SYSTEM_ADMIN")).isEmpty();
    }
}
