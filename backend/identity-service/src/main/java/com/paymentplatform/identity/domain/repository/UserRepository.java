package com.paymentplatform.identity.domain.repository;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.PageResult;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

/** Port de persistance des utilisateurs (implémenté en infrastructure). */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByUsername(Username username);

    Optional<User> findByEmail(Email email);

    List<User> findByOrganizationId(OrganizationId organizationId);

    List<User> findByOrganizationIdAndRole(OrganizationId organizationId, RoleCode role);

    /**
     * B5 : remplace {@code findAll()} pour les listes exposées — recherche
     * filtrée et paginée en base (page 0-based, taille déjà bornée par l'appelant).
     * Paramètres de filtre nullables.
     */
    PageResult<User> findPage(OrganizationId organizationId, String status, RoleCode role, int page, int size);

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    User save(User user);
}