package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.RelationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.RelationStatus;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupplierShopRelationUseCase {

    private final SupplierShopRelationRepository relations;
    private final OrganizationRepository organizations;
    private final AuditRecorder audit;

    public SupplierShopRelationUseCase(SupplierShopRelationRepository relations,
                                       OrganizationRepository organizations,
                                       AuditRecorder audit) {
        this.relations = relations;
        this.organizations = organizations;
        this.audit = audit;
    }

    @Transactional
    public RelationResponse createRelation(CreateRelationRequest request) {
        OrganizationId supplierId = OrganizationId.of(request.supplierId());
        OrganizationId shopId = OrganizationId.of(request.shopId());

        Organization supplier = organizations.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Fournisseur non trouvé : " + request.supplierId()));
        if (!supplier.isSupplier()) {
            throw new ConflictException("L'organisation " + request.supplierId() + " n'est pas un fournisseur");
        }
        if (!supplier.isActive()) {
            throw new ConflictException("Le fournisseur " + request.supplierId() + " est désactivé");
        }

        Organization shop = organizations.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + request.shopId()));
        if (!shop.isShop()) {
            throw new ConflictException("L'organisation " + request.shopId() + " n'est pas une boutique");
        }
        if (!shop.isActive()) {
            throw new ConflictException("La boutique " + request.shopId() + " est désactivée");
        }

        if (relations.existsBySupplierIdAndShopIdAndStatus(supplierId, shopId, RelationStatus.ACTIVE)) {
            throw new ConflictException("La relation entre le fournisseur " + request.supplierId()
                    + " et la boutique " + request.shopId() + " existe déjà");
        }

        Optional<SupplierShopRelation> existing = relations.findBySupplierIdAndStatus(supplierId, RelationStatus.INACTIVE).stream()
                .filter(r -> r.shopId().equals(shopId))
                .findFirst();

        SupplierShopRelation saved;
        if (existing.isPresent()) {
            existing.get().activate();
            saved = relations.save(existing.get());
        } else {
            SupplierShopRelation relation = SupplierShopRelation.create(supplierId, shopId);
            saved = relations.save(relation);
        }

        audit.record(null, supplierId.value(), "RELATION_CREATED", saved.id(),
                "{\"supplierId\":" + request.supplierId() + ",\"shopId\":" + request.shopId() + "}");

        return RelationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RelationResponse> listBySupplier(UUID supplierId) {
        return relations.findBySupplierId(OrganizationId.of(supplierId)).stream()
                .map(RelationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RelationResponse> listByShop(UUID shopId) {
        return relations.findByShopId(OrganizationId.of(shopId)).stream()
                .map(RelationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RelationResponse> listAll() {
        return relations.findByStatus(RelationStatus.ACTIVE).stream()
                .map(RelationResponse::from).toList();
    }

    @Transactional
    public void deactivateRelation(UUID relationId) {
        SupplierShopRelation relation = relations.findById(relationId)
                .orElseThrow(() -> new NotFoundException("Relation non trouvée : " + relationId));
        relation.deactivate();
        relations.save(relation);

        audit.record(null, relation.supplierId().value(), "RELATION_DEACTIVATED", relationId,
                "{\"supplierId\":" + relation.supplierId().value() + ",\"shopId\":" + relation.shopId().value() + "}");
    }

    @Transactional(readOnly = true)
    public boolean existsActiveRelation(UUID supplierId, UUID shopId) {
        return relations.existsBySupplierIdAndShopIdAndStatus(
                OrganizationId.of(supplierId), OrganizationId.of(shopId), RelationStatus.ACTIVE);
    }
}
