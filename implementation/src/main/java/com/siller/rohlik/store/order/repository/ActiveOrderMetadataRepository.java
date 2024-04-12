package com.siller.rohlik.store.order.repository;

import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface ActiveOrderMetadataRepository extends JpaRepository<ActiveOrderMetadata, Long> {

    void deleteByOrder(Order order);

    List<ActiveOrderMetadata> findAllByCreatedAtBefore(Timestamp timestamp);
}
