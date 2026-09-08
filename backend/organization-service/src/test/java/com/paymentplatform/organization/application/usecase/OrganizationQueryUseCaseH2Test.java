package com.paymentplatform.organization.application.usecase;

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
        createOrg.execute(new CreateOrganizationRequest("Supplier A", "SUPPLIER"), 1L);
        createOrg.execute(new CreateOrganizationRequest("Supplier B", "SUPPLIER"), 1L);
        createOrg.execute(new CreateOrganizationRequest("Shop A", "SHOP"), 1L);
    }

    @Test
    void listAll_returnsAll() {
        var result = query.listAll();
        assertThat(result).hasSize(3);
    }

    @Test
    void listByType_supplier() {
        var result = query.listByType("SUPPLIER");
        assertThat(result).hasSize(2);
    }

    @Test
    void listByType_shop() {
        var result = query.listByType("SHOP");
        assertThat(result).hasSize(1);
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
        assertThatThrownBy(() -> query.findById(999L))
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
