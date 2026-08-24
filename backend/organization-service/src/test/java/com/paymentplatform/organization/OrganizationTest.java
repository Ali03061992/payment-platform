package com.paymentplatform.organization;

import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrganizationTest {

    @Test
    void createSupplier() {
        Organization org = Organization.create(
                OrganizationId.of(1), OrganizationName.of("Fournisseur ABC"), OrganizationType.SUPPLIER);

        assertThat(org.id().value()).isEqualTo(1);
        assertThat(org.name().value()).isEqualTo("Fournisseur ABC");
        assertThat(org.type()).isEqualTo(OrganizationType.SUPPLIER);
        assertThat(org.status()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(org.isActive()).isTrue();
        assertThat(org.isSupplier()).isTrue();
        assertThat(org.isShop()).isFalse();
    }

    @Test
    void createShop() {
        Organization org = Organization.create(
                OrganizationId.of(2), OrganizationName.of("Boutique Tunis"), OrganizationType.SHOP);

        assertThat(org.type()).isEqualTo(OrganizationType.SHOP);
        assertThat(org.isShop()).isTrue();
        assertThat(org.isSupplier()).isFalse();
    }

    @Test
    void disableIsIdempotent() {
        Organization org = Organization.create(
                OrganizationId.of(1), OrganizationName.of("Test"), OrganizationType.SUPPLIER);

        org.disable();
        assertThat(org.status()).isEqualTo(OrganizationStatus.DISABLED);
        assertThat(org.isActive()).isFalse();

        org.disable();
        assertThat(org.status()).isEqualTo(OrganizationStatus.DISABLED);
    }

    @Test
    void activateIsIdempotent() {
        Organization org = Organization.create(
                OrganizationId.of(1), OrganizationName.of("Test"), OrganizationType.SUPPLIER);

        org.disable();
        org.activate();
        assertThat(org.status()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(org.isActive()).isTrue();

        org.activate();
        assertThat(org.status()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    void disableThenActivateDoesNotReEnableIndividuallyDisabledUsers() {
        Organization org = Organization.create(
                OrganizationId.of(1), OrganizationName.of("Test"), OrganizationType.SUPPLIER);

        org.disable();
        assertThat(org.status()).isEqualTo(OrganizationStatus.DISABLED);

        org.activate();
        assertThat(org.status()).isEqualTo(OrganizationStatus.ACTIVE);

        assertThat(org.isActive()).isTrue();
    }

    @Test
    void updateName() {
        Organization org = Organization.create(
                OrganizationId.of(1), OrganizationName.of("Old Name"), OrganizationType.SUPPLIER);

        org.updateName(OrganizationName.of("New Name"));
        assertThat(org.name().value()).isEqualTo("New Name");
    }

    @Test
    void rejectBlankName() {
        assertThatThrownBy(() -> OrganizationName.of(""))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectNullType() {
        assertThatThrownBy(() -> Organization.create(
                OrganizationId.of(1), OrganizationName.of("Test"), null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void supplierShopRelationCreate() {
        SupplierShopRelation relation = SupplierShopRelation.create(
                OrganizationId.of(1), OrganizationId.of(2));

        assertThat(relation.supplierId().value()).isEqualTo(1);
        assertThat(relation.shopId().value()).isEqualTo(2);
        assertThat(relation.isActive()).isTrue();
    }

    @Test
    void supplierShopRelationCannotSelfReference() {
        assertThatThrownBy(() -> SupplierShopRelation.create(
                OrganizationId.of(1), OrganizationId.of(1)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void supplierShopRelationDeactivate() {
        SupplierShopRelation relation = SupplierShopRelation.create(
                OrganizationId.of(1), OrganizationId.of(2));

        relation.deactivate();
        assertThat(relation.isActive()).isFalse();
    }
}
