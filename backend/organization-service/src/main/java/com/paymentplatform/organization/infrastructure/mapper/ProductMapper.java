package com.paymentplatform.organization.infrastructure.mapper;

import com.paymentplatform.organization.application.dto.ProductResponse;
import com.paymentplatform.organization.domain.model.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);
}
