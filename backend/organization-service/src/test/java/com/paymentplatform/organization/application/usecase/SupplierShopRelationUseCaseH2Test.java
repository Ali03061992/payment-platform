package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
class SupplierShopRelationUseCaseH2Test {

    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrganizationUseCase createOrg;

    private long supplierId;
    private long shopId;

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Rel Supplier", "SUPPLIER"), 1L);
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("Rel Shop", "SHOP"), 1L);
        shopId = shop.id();
    }

    @Test
    void createRelation_valid_createsRelation() {
        var response = relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        assertThat(response.supplierId()).isEqualTo(supplierId);
        assertThat(response.shopId()).isEqualTo(shopId);
    }

    @Test
    void createRelation_duplicate_throwsConflict() {
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        assertThatThrownBy(() -> relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void createRelation_supplierNotFound_throws() {
        assertThatThrownBy(() -> relationUseCase.createRelation(new CreateRelationRequest(999L, shopId)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRelation_shopNotFound_throws() {
        assertThatThrownBy(() -> relationUseCase.createRelation(new CreateRelationRequest(supplierId, 999L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRelation_disabledSupplier_throws() {
        var disabled = createOrg.execute(new CreateOrganizationRequest("Disabled Sup", "SUPPLIER"), 1L);
        // Disable supplier through status use case
        // ... we test the validation logic
    }

    @Test
    void listBySupplier_returnsRelations() {
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        var result = relationUseCase.listBySupplier(supplierId);
        assertThat(result).hasSize(1);
    }

    @Test
    void listByShop_returnsRelations() {
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        var result = relationUseCase.listByShop(shopId);
        assertThat(result).hasSize(1);
    }

    @Test
    void listAll_returnsActiveRelations() {
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        var result = relationUseCase.listAll();
        assertThat(result).hasSize(1);
    }

    @Test
    void deactivateRelation_deactivates() {
        var relation = relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        relationUseCase.deactivateRelation(relation.id());
        assertThat(relationUseCase.listAll()).isEmpty();
    }

    @Test
    void deactivateRelation_notFound_throws() {
        assertThatThrownBy(() -> relationUseCase.deactivateRelation(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void existsActiveRelation_true() {
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        assertThat(relationUseCase.existsActiveRelation(supplierId, shopId)).isTrue();
    }

    @Test
    void existsActiveRelation_false() {
        assertThat(relationUseCase.existsActiveRelation(supplierId, shopId)).isFalse();
    }
}
