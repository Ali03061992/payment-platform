package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Consultation des utilisateurs avec respect du périmètre d'accès. */
@Service
public class UserQueryUseCase {

    private final UserRepository users;

    public UserQueryUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID actorUserId, UUID userId, List<String> actorRoles,
                                 UUID actorOrganizationId) {
        User target = users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        assertCanView(actorRoles, actorOrganizationId, target);
        return UserResponse.from(target);
    }

    @Transactional(readOnly = true)
    public UserResponse findByIdInternal(UUID userId) {
        User target = users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        return UserResponse.from(target);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(UUID actorUserId, List<String> actorRoles, UUID actorOrganizationId,
                                   UUID organizationId, String role, String status) {
        List<User> result;
        boolean isSystemAdmin = actorRoles.contains(RoleCode.SYSTEM_ADMIN.name());
        if (isSystemAdmin) {
            result = organizationId == null ? users.findAll()
                    : users.findByOrganizationId(OrganizationId.of(organizationId));
        } else {
            if (actorOrganizationId == null) {
                throw new ForbiddenException("Accès hors périmètre");
            }
            result = users.findByOrganizationId(OrganizationId.of(actorOrganizationId));
        }
        return result.stream()
                .filter(u -> role == null || u.roles().contains(RoleCode.from(role)))
                .filter(u -> status == null || u.status() == UserStatus.valueOf(status))
                .map(UserResponse::from)
                .toList();
    }

    private void assertCanView(List<String> actorRoles, UUID actorOrganizationId, User target) {
        if (actorRoles.contains(RoleCode.SYSTEM_ADMIN.name())) {
            return;
        }
        if (actorOrganizationId == null || target.organizationId() == null
                || !actorOrganizationId.equals(target.organizationId().value())) {
            throw new ForbiddenException("Accès hors périmètre");
        }
    }
}