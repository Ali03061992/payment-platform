package com.paymentplatform.organization.infrastructure.mapper;

import com.paymentplatform.organization.application.dto.RelationResponse;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupplierShopRelationMapper {

    @Mapping(target = "id", expression = "java(relation.id())")
    @Mapping(target = "supplierId", expression = "java(relation.supplierId() != null ? relation.supplierId().value() : null)")
    @Mapping(target = "shopId", expression = "java(relation.shopId() != null ? relation.shopId().value() : null)")
    @Mapping(target = "status", expression = "java(relation.status() != null ? relation.status().name() : \"\")")
    @Mapping(target = "createdAt", expression = "java(relation.createdAt())")
    RelationResponse toResponse(SupplierShopRelation relation);
}
