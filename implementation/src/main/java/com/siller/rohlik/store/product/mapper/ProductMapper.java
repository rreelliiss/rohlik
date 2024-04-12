package com.siller.rohlik.store.product.mapper;

import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.rest.model.product.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product fromDto(ProductDto dto);
}
