package com.siller.rohlik.store.product.repository;

import com.siller.rohlik.store.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
