package com.paymentplatform.organization.domain.model;

import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationName;
import com.paymentplatform.organization.domain.valueobject.OrganizationStatus;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.shared.domain.exception.DomainException;

import java.time.Instant;

/**
 * Aggregate Root Organization.
 * Invariants : nom unique, type SUPPLIER|SHOP, statut ACTIVE|DISABLED.
 * La désactivation est idempotente, la réactivation est idempotente.
 */
public class Organization {

    private final OrganizationId id;
    private OrganizationName name;
    private final OrganizationType type;
    private OrganizationStatus status;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Organization(OrganizationId id, OrganizationName name, OrganizationType type,
                         OrganizationStatus status, long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Organization create(OrganizationId id, OrganizationName name, OrganizationType type) {
        if (name == null) {
            throw new DomainException("Le nom est requis");
        }
        if (type == null) {
            throw new DomainException("Le type est requis");
        }
        Instant now = Instant.now();
        return new Organization(id, name, type, OrganizationStatus.ACTIVE, 0, now, now);
    }

    public static Organization reconstruct(OrganizationId id, OrganizationName name, OrganizationType type,
                                           OrganizationStatus status, long version,
                                           Instant createdAt, Instant updatedAt) {
        return new Organization(id, name, type, status, version, createdAt, updatedAt);
    }

    public void disable() {
        if (status == OrganizationStatus.DISABLED) {
            return;
        }
        this.status = OrganizationStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status == OrganizationStatus.ACTIVE) {
            return;
        }
        this.status = OrganizationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void updateName(OrganizationName newName) {
        if (newName == null) {
            throw new DomainException("Le nom est requis");
        }
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }

    public boolean isSupplier() {
        return type == OrganizationType.SUPPLIER;
    }

    public boolean isShop() {
        return type == OrganizationType.SHOP;
    }

    public OrganizationId id() { return id; }
    public OrganizationName name() { return name; }
    public OrganizationType type() { return type; }
    public OrganizationStatus status() { return status; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "Organization{id=" + id + ", name=" + name + ", type=" + type + ", status=" + status + "}";
    }
}
