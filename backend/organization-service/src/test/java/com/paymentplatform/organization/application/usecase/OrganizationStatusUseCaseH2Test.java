package com.paymentplatform.organization.application.usecase;

import java.util.UUID;

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

    private UUID supplierId;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Test Supplier", "SUPPLIER"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("Test Shop", "SHOP"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shopId = shop.id();
    }

    @Test
    void disable_supplier_becomesDisabled() {
        var response = status.disable(supplierId, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void activate_disabledSupplier_becomesActive() {
        status.disable(supplierId, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var response = status.activate(supplierId, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void disable_shop_becomesDisabled() {
        var response = status.disable(shopId, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void activate_disabledShop_becomesActive() {
        status.disable(shopId, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var response = status.activate(shopId, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void disable_notFound_throws() {
        assertThatThrownBy(() -> status.disable(UUID.fromString("00000000-0000-0000-0000-000000000999"), UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void activate_notFound_throws() {
        assertThatThrownBy(() -> status.activate(UUID.fromString("00000000-0000-0000-0000-000000000999"), UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(NotFoundException.class);
    }
}
