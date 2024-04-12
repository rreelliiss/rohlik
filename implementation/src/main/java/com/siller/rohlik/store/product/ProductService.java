package com.siller.rohlik.store.product;

import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.OrderItem;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product saveProduct(Product product) {
        product = productRepository.save(product);
        return product;
    }

    @Transactional
    public void returnProducts(Order order) {
        for (OrderItem orderItem : order.getItems()) {
            Optional<Product> potentialProduct = productRepository.findById(orderItem.getProduct().getId());
            if (potentialProduct.isPresent()) {
                Product product = potentialProduct.get();
                product.setQuantity(product.getQuantity() + orderItem.getQuantity());
                productRepository.save(product);
            }
        }
    }

    public void deleteById(String id) {
        productRepository.deleteById(id);
    }


    public boolean existsById(String id) {
        return productRepository.existsById(id);
    }
}
