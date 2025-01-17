package com.siller.rohlik.store.order.repository;

import com.siller.rohlik.store.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
}
