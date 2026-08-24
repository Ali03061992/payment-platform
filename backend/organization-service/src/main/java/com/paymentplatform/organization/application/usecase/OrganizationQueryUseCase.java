package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationQueryUseCase {

    private final OrganizationRepository organizations;
    private final SupplierShopRelationRepository relations;

    public OrganizationQueryUseCase(OrganizationRepository organizations,
                                    SupplierShopRelationRepository relations) {
        this.organizations = organizations;
        this.relations = relations;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listAll() {
        return organizations.findAll().stream()
                .map(org -> OrganizationResponse.from(org, getRelations(org)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listByType(String type) {
        OrganizationType orgType = OrganizationType.from(type);
        return organizations.findByType(orgType).stream()
                .map(org -> OrganizationResponse.from(org, getRelations(org)))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse findById(long id) {
        Organization org = organizations.findById(OrganizationId.of(id))
                .orElseThrow(() -> new NotFoundException("Organisation non trouvée : " + id));
        return OrganizationResponse.from(org, getRelations(org));
    }

    @Transactional(readOnly = true)
    public long countSuppliers() {
        return organizations.countByType(OrganizationType.SUPPLIER);
    }

    @Transactional(readOnly = true)
    public long countShops() {
        return organizations.countByType(OrganizationType.SHOP);
    }

    private List<SupplierShopRelation> getRelations(Organization org) {
        if (org.isSupplier()) {
            return relations.findBySupplierId(org.id());
        } else {
            return relations.findByShopId(org.id());
        }
    }
}
