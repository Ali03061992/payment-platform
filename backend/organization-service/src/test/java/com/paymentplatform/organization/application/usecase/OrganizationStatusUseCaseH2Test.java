package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
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
class OrganizationStatusUseCaseH2Test {

    @Autowired private OrganizationStatusUseCase status;
    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private OrganizationQueryUseCase query;

    private long supplierId;
    private long shopId;

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Test Supplier", "SUPPLIER"), 1L);
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("Test Shop", "SHOP"), 1L);
        shopId = shop.id();
    }

    @Test
    void disable_supplier_becomesDisabled() {
        var response = status.disable(supplierId, 1L);
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void activate_disabledSupplier_becomesActive() {
        status.disable(supplierId, 1L);
        var response = status.activate(supplierId, 1L);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void disable_shop_becomesDisabled() {
        var response = status.disable(shopId, 1L);
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void activate_disabledShop_becomesActive() {
        status.disable(shopId, 1L);
        var response = status.activate(shopId, 1L);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void disable_notFound_throws() {
        assertThatThrownBy(() -> status.disable(999L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void activate_notFound_throws() {
        assertThatThrownBy(() -> status.activate(999L, 1L))
                .isInstanceOf(NotFoundException.class);
    }
}
