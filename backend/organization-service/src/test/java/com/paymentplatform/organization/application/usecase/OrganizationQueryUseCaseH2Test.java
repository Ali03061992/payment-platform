package com.paymentplatform.organization.application.usecase;

import java.util.UUID;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.OrganizationResponse;
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
class OrganizationQueryUseCaseH2Test {

    @Autowired private OrganizationQueryUseCase query;
    @Autowired private CreateOrganizationUseCase createOrg;

    @BeforeEach
    void setUp() {
        createOrg.execute(new CreateOrganizationRequest("Supplier A", "SUPPLIER"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createOrg.execute(new CreateOrganizationRequest("Supplier B", "SUPPLIER"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createOrg.execute(new CreateOrganizationRequest("Shop A", "SHOP"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void listAll_returnsAll() {
        var result = query.listAll();
        assertThat(result).hasSize(3);
    }

    @Test
    void listByType_supplier() {
        var result = query.listByType("SUPPLIER", 0, 20);
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void listByType_shop() {
        var result = query.listByType("SHOP", 0, 20);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listByType_pagination() {
        var page0 = query.listByType("SUPPLIER", 0, 1);
        assertThat(page0.items()).hasSize(1);
        assertThat(page0.totalElements()).isEqualTo(2);
        assertThat(page0.totalPages()).isEqualTo(2);
        var page1 = query.listByType("SUPPLIER", 1, 1);
        assertThat(page1.items()).hasSize(1);
        assertThat(page1.items().get(0).id())
                .isNotEqualTo(page0.items().get(0).id());
    }

    @Test
    void findById_validId() {
        var all = query.listAll();
        var first = all.getFirst();
        var found = query.findById(first.id());
        assertThat(found.name()).isEqualTo(first.name());
    }

    @Test
    void findById_invalidId_throwsNotFound() {
        assertThatThrownBy(() -> query.findById(UUID.fromString("00000000-0000-0000-0000-000000000999")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void countSuppliers() {
        assertThat(query.countSuppliers()).isEqualTo(2);
    }

    @Test
    void countShops() {
        assertThat(query.countShops()).isEqualTo(1);
    }
}
