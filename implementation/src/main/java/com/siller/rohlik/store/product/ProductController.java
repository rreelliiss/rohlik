package com.siller.rohlik.store.product;

import com.siller.rohlik.store.product.mapper.ProductMapper;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.api.ProductApi;
import com.siller.rohlik.store.rest.model.CreateNewProductResponseDto;
import com.siller.rohlik.store.rest.model.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ResponseEntity<CreateNewProductResponseDto> createNewProduct(ProductDto productDto) {
        Product product = productMapper.fromDto(productDto);
        product = productRepository.save(product);
        return new ResponseEntity<>(new CreateNewProductResponseDto(product.getId()), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteProduct(String id) {
        return null;
    }

    @Override
    public ResponseEntity<Void> setProduct(String id, ProductDto productDto) {
        return null;
    }
}
