package com.siller.rohlik.store.order.model;

import com.siller.rohlik.store.product.model.Product;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class OrderItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Product product;

    private Integer quantity;

    @ManyToOne
    @JoinColumn(name="order_id", nullable=false)
    private Order order;
}
