package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Adapter JPA du port UserRepository. */
@Component
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(Username username) {
        return jpa.findByUsername(username.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByOrganizationId(OrganizationId organizationId) {
        return jpa.findByOrganizationId(organizationId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByOrganizationIdAndRole(OrganizationId organizationId, RoleCode role) {
        return findByOrganizationId(organizationId).stream()
                .filter(u -> u.hasRole(role))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(Username username) {
        return jpa.existsByUsername(username.value());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity e = new UserJpaEntity();
        if (user.id().value() > 0) {
            e.setId(user.id().value());
            e.setVersion(user.version());
        }
        e.setUsername(user.username().value());
        e.setEmail(user.email().value());
        e.setPasswordHash(user.password().value());
        e.setFirstName(user.firstName());
        e.setLastName(user.lastName());
        e.setPhone(user.phone() == null ? null : user.phone().value());
        e.setOrganizationId(user.organizationId() == null ? null : user.organizationId().value());
        e.setStatus(user.status().name());
        e.setCreatedAt(user.createdAt());
        e.setUpdatedAt(user.updatedAt());
        e.setRoles(EnumSet.copyOf(user.roles()));
        return e;
    }

    private User toDomain(UserJpaEntity e) {
        Set<RoleCode> roles = e.getRoles() == null ? EnumSet.noneOf(RoleCode.class) : e.getRoles();
        return User.reconstruct(UserId.of(e.getId()),
                new Username(e.getUsername()),
                new Email(e.getEmail()),
                new PasswordHash(e.getPasswordHash()),
                e.getFirstName(),
                e.getLastName(),
                new PhoneNumber(e.getPhone()),
                e.getOrganizationId() == null ? null : OrganizationId.of(e.getOrganizationId()),
                UserStatus.valueOf(e.getStatus()),
                roles,
                e.getVersion() == null ? 0 : e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}