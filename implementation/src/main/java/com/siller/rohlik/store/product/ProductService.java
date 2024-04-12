package com.siller.rohlik.store.product;

import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.OrderItem;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;


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
}
