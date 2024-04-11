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
        productRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> setProduct(String id, ProductDto productDto) {
        boolean productExists = productRepository.existsById(id);
        if (productExists) {
            Product product = productMapper.fromDto(productDto);
            product.setId(id);
            productRepository.save(product);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
