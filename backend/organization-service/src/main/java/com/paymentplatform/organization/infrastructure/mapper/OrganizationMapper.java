package com.paymentplatform.organization.infrastructure.mapper;

import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", expression = "java(org.id() != null ? org.id().value() : 0L)")
    @Mapping(target = "name", expression = "java(org.name() != null ? org.name().value() : \"\")")
    @Mapping(target = "type", expression = "java(org.type() != null ? org.type().name() : \"\")")
    @Mapping(target = "status", expression = "java(org.status() != null ? org.status().name() : \"\")")
    @Mapping(target = "relations", ignore = true)
    OrganizationResponse toResponse(Organization org);
}
