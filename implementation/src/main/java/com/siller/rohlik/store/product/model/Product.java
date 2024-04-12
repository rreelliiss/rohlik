package com.siller.rohlik.store.product.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(length = 256)
    private String name;
    private Integer quantity;
    private BigDecimal price;

    public boolean doesNotHaveEnoughQuantity(int quantity) {
        return this.quantity < quantity;
    }

    public boolean isNotFinished(){
        return quantity == null || price == null;
    }
}
