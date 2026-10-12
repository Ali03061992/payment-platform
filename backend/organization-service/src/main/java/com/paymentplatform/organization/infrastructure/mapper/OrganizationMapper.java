package com.paymentplatform.organization.infrastructure.mapper;

import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", expression = "java(org.id() != null ? org.id().value() : null)")
    @Mapping(target = "name", expression = "java(org.name() != null ? org.name().value() : \"\")")
    @Mapping(target = "type", expression = "java(org.type() != null ? org.type().name() : \"\")")
    @Mapping(target = "status", expression = "java(org.status() != null ? org.status().name() : \"\")")
    @Mapping(target = "relations", ignore = true)
    @Mapping(target = "matriculeFiscal", expression = "java(org.matriculeFiscal())")
    @Mapping(target = "patente", expression = "java(org.patente())")
    @Mapping(target = "tejIdentifier", expression = "java(org.tejIdentifier())")
    @Mapping(target = "defaultWithholdingNature", expression = "java(org.defaultWithholdingNature())")
    @Mapping(target = "address", expression = "java(org.address())")
    @Mapping(target = "fiscalRegime", expression = "java(org.fiscalRegime() != null ? org.fiscalRegime().name() : null)")
    @Mapping(target = "suspensionAttestationNumber", expression = "java(org.suspensionAttestationNumber())")
    @Mapping(target = "suspensionAttestationDate", expression = "java(org.suspensionAttestationDate())")
    @Mapping(target = "suspensionExpiryDate", expression = "java(org.suspensionExpiryDate())")
    @Mapping(target = "version", expression = "java(org.version())")
    @Mapping(target = "createdAt", expression = "java(org.createdAt())")
    @Mapping(target = "updatedAt", expression = "java(org.updatedAt())")
    OrganizationResponse toResponse(Organization org);
}
