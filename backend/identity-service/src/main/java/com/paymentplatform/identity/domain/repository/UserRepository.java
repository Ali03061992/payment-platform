package com.paymentplatform.identity.domain.repository;

import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.Username;

import java.util.List;
import java.util.Optional;

/** Port de persistance des utilisateurs (implémenté en infrastructure). */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByUsername(Username username);

    Optional<User> findByEmail(Email email);

    List<User> findByOrganizationId(OrganizationId organizationId);

    List<User> findByOrganizationIdAndRole(OrganizationId organizationId, RoleCode role);

    List<User> findAll();

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    User save(User user);
}