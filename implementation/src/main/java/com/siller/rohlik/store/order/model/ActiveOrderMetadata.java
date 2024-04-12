package com.siller.rohlik.store.order.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
public class ActiveOrderMetadata {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private Order order;

    private Timestamp createdAt;

    public ActiveOrderMetadata(Order order, Timestamp createdAt) {
        this.order = order;
        this.createdAt = createdAt;
    }
}
