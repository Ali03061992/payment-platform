package com.paymentplatform.organization.infrastructure.mapper;

import com.paymentplatform.organization.application.dto.RelationResponse;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SupplierShopRelationMapper {

    @Mapping(target = "id", expression = "java(relation.id())")
    @Mapping(target = "supplierId", expression = "java(relation.supplierId() != null ? relation.supplierId().value() : 0L)")
    @Mapping(target = "shopId", expression = "java(relation.shopId() != null ? relation.shopId().value() : 0L)")
    @Mapping(target = "status", expression = "java(relation.status() != null ? relation.status().name() : \"\")")
    RelationResponse toResponse(SupplierShopRelation relation);
}
