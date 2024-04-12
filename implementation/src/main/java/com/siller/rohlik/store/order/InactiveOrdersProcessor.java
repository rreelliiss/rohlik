package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.repository.ActiveOrderMetadataRepository;
import com.siller.rohlik.store.order.repository.OrderRepository;
import com.siller.rohlik.store.product.ProductService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
@Slf4j
public class InactiveOrdersProcessor {

    private final OrderRepository orderRepository;
    private final ActiveOrderMetadataRepository activeOrderMetadataRepository;

    private final Integer activePaymentsShouldBeInvalidatedAfterSeconds;
    private final ProductService productService;

    public InactiveOrdersProcessor(
            @Autowired  OrderRepository orderRepository,
            @Autowired  ActiveOrderMetadataRepository activeOrderMetadataRepository,
            @Value("${activePaymentsShouldBeInvalidatedAfterSeconds}") Integer activePaymentsShouldBeInvalidatedAfterSeconds, ProductService productService) {
        this.orderRepository = orderRepository;
        this.activeOrderMetadataRepository = activeOrderMetadataRepository;
        this.activePaymentsShouldBeInvalidatedAfterSeconds = activePaymentsShouldBeInvalidatedAfterSeconds;
        this.productService = productService;
    }

    @Scheduled(cron = "${inactiveOrdersProcessor.cronExpression}")
    @Transactional
    public void scheduleTaskUsingCronExpression() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - 1000L * activePaymentsShouldBeInvalidatedAfterSeconds);
        List<ActiveOrderMetadata> metadataOfOrdersToInActivate = activeOrderMetadataRepository.findAllByCreatedAtBefore(timestamp);
        for(ActiveOrderMetadata orderMetadata : metadataOfOrdersToInActivate) {
            invalidateOrder(orderMetadata);
            log.debug("Invalidating inactive order: {}", orderMetadata);
        }
        log.trace("schedule tasks using cron jobs - " + timestamp);
    }

    private void invalidateOrder(ActiveOrderMetadata orderMetadata) {
        orderMetadata.getOrder().setState(Order.State.INVALIDATED);
        productService.returnProducts(orderMetadata.getOrder());
        orderRepository.save(orderMetadata.getOrder());
        activeOrderMetadataRepository.delete(orderMetadata);
    }
}
