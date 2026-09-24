package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.application.dto.PageResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationQueryUseCase {

    /** B5 : taille de page plafonnée — aucune liste exposée ne charge plus de 100 lignes. */
    public static final int MAX_PAGE_SIZE = 100;

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
    public PageResponse<OrganizationResponse> listByType(String type, int page, int size) {
        OrganizationType orgType = OrganizationType.from(type);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageResult<Organization> result = organizations.findByType(orgType, safePage, safeSize);
        List<OrganizationResponse> items = result.items().stream()
                .map(org -> OrganizationResponse.from(org, getRelations(org)))
                .toList();
        int totalPages = (int) Math.ceil((double) result.totalElements() / safeSize);
        return new PageResponse<>(items, result.totalElements(), totalPages, safePage);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse findById(UUID id) {
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
