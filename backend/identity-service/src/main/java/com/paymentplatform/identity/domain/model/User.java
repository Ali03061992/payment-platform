package com.paymentplatform.identity.domain.model;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Aggregate Root User.
 * Invariants : username/email uniques ; statut ACTIVE|DISABLED ;
 * SYSTEM_ADMIN sans organisation ; la désactivation est idempotente.
 * Ne dépend d'aucun framework (DDD).
 */
public class User {

    private final UserId id;
    private final Username username;
    private Email email;
    private PasswordHash password;
    private String firstName;
    private String lastName;
    private PhoneNumber phone;
    private final OrganizationId organizationId;
    private UserStatus status;
    private final Set<RoleCode> roles;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    private User(UserId id, Username username, Email email, PasswordHash password, String firstName,
                 String lastName, PhoneNumber phone, OrganizationId organizationId, UserStatus status,
                 Set<RoleCode> roles, long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.organizationId = organizationId;
        this.status = status;
        this.roles = roles;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(UserId id, Username username, Email email, PasswordHash password,
                              String firstName, String lastName, PhoneNumber phone,
                              OrganizationId organizationId, RoleCode role) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new DomainException("Nom et prénom obligatoires");
        }
        Set<RoleCode> roles = EnumSet.of(role);
        Instant now = Instant.now();
        return new User(id, username, email, password, firstName.trim(), lastName.trim(), phone,
                organizationId, UserStatus.ACTIVE, roles, 0, now, now);
    }

    public static User reconstruct(UserId id, Username username, Email email, PasswordHash password,
                                    String firstName, String lastName, PhoneNumber phone,
                                    OrganizationId organizationId, UserStatus status, Set<RoleCode> roles,
                                    long version, Instant createdAt, Instant updatedAt) {
        return new User(id, username, email, password, firstName, lastName, phone, organizationId, status,
                roles, version, createdAt, updatedAt);
    }

    public UserId id() {
        return id;
    }

    public Username username() {
        return username;
    }

    public Email email() {
        return email;
    }

    public PasswordHash password() {
        return password;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public UserStatus status() {
        return status;
    }

    public Set<RoleCode> roles() {
        return EnumSet.copyOf(roles);
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    /** Désactivation individuelle, idempotente. */
    public void disable() {
        if (status == UserStatus.DISABLED) {
            return;
        }
        this.status = UserStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status == UserStatus.ACTIVE) {
            return;
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String firstName, String lastName, PhoneNumber phone) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new DomainException("Nom et prénom obligatoires");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.phone = phone;
        this.updatedAt = Instant.now();
    }

    public void changePassword(PasswordHash newPassword) {
        if (newPassword == null) {
            throw new DomainException("Mot de passe manquant");
        }
        this.password = newPassword;
        this.updatedAt = Instant.now();
    }

    public void updateEmail(Email email) {
        if (email == null) {
            throw new DomainException("Email manquant");
        }
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public void addRole(RoleCode role) {
        if (role == null) {
            throw new DomainException("Rôle manquant");
        }
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    /** Vérification que l'utilisateur peut agir pour cette organisation. */
    public void assertCanManageOrganization(OrganizationId target) {
        if (organizationId == null) {
            throw new ConflictException("Un SYSTEM_ADMIN n'est rattaché à aucune organisation");
        }
        if (!organizationId.equals(target)) {
            throw new ConflictException("Accès hors périmètre : organisation " + target + " non autorisée");
        }
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username=" + username + ", status=" + status + "}";
    }
}