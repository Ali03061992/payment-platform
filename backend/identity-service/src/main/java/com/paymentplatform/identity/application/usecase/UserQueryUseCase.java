package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.PageResponse;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.PageResult;
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
    public List<UserResponse> listByOrganizationInternal(UUID organizationId) {
        return users.findByOrganizationId(OrganizationId.of(organizationId)).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** B5 : taille de page plafonnée — aucune liste exposée ne charge plus de 100 lignes. */
    public static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(UUID actorUserId, List<String> actorRoles, UUID actorOrganizationId,
                                           UUID organizationId, String role, String status, int page, int size) {
        boolean isSystemAdmin = actorRoles.contains(RoleCode.SYSTEM_ADMIN.name());
        UUID scopeOrg;
        if (isSystemAdmin) {
            scopeOrg = organizationId;
        } else {
            if (actorOrganizationId == null) {
                throw new ForbiddenException("Accès hors périmètre");
            }
            scopeOrg = actorOrganizationId;
        }
        // Mêmes validations qu'avant (valeur inconnue => exception), filtres poussés en SQL.
        RoleCode roleCode = role == null ? null : RoleCode.from(role);
        String statusCode = null;
        if (status != null) {
            statusCode = UserStatus.valueOf(status).name();
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageResult<User> result = users.findPage(
                scopeOrg == null ? null : OrganizationId.of(scopeOrg),
                statusCode, roleCode, safePage, safeSize);
        int totalPages = (int) Math.ceil((double) result.totalElements() / safeSize);
        return new PageResponse<>(result.items().stream().map(UserResponse::from).toList(),
                result.totalElements(), totalPages, safePage);
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