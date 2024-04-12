package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.product.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
public class InactiveOrdersProcessor {

    private final OrderRepository orderRepository;
    private final ActiveOrderMetadataRepository activeOrderMetadataRepository;

    private final Integer activePaymenetsShouldBeInvalidatedAfterSeconds;
    private final ProductService productService;

    public InactiveOrdersProcessor(
            @Autowired  OrderRepository orderRepository,
            @Autowired  ActiveOrderMetadataRepository activeOrderMetadataRepository,
            @Value("${activePaymentsShouldBeInvalidatedAfterSeconds}") Integer activePaymentsShouldBeInvalidatedAfterSeconds, ProductService productService) {
        this.orderRepository = orderRepository;
        this.activeOrderMetadataRepository = activeOrderMetadataRepository;
        this.activePaymenetsShouldBeInvalidatedAfterSeconds = activePaymentsShouldBeInvalidatedAfterSeconds;
        this.productService = productService;
    }

    @Scheduled(cron = "${inactiveOrdersProcessor.cronExpression}")
    @Transactional
    public void scheduleTaskUsingCronExpression() {
        long now = System.currentTimeMillis() / 1000;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - 1000L * activePaymenetsShouldBeInvalidatedAfterSeconds);
        List<ActiveOrderMetadata> metadataOfOrdersToInActivate = activeOrderMetadataRepository.findAllByCreatedAtBefore(timestamp);
        List<ActiveOrderMetadata> all = activeOrderMetadataRepository.findAll();
        for(ActiveOrderMetadata orderMetadata : metadataOfOrdersToInActivate) {
            orderMetadata.getOrder().setState(Order.State.INVALIDATED);
            productService.returnProducts(orderMetadata.getOrder());
            orderRepository.save(orderMetadata.getOrder());
            activeOrderMetadataRepository.delete(orderMetadata);
        }
        System.out.println(
                "schedule tasks using cron jobs - " + now);
    }
}
