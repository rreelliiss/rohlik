package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.CreateNewOrderError;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.OrderItem;
import com.siller.rohlik.store.order.repository.ActiveOrderMetadataRepository;
import com.siller.rohlik.store.order.repository.OrderRepository;
import com.siller.rohlik.store.product.ProductService;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.order.OrderDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductService productService;

    private final OrderRepository orderRepository;
    private final ActiveOrderMetadataRepository activeOrderMetadataRepository;
    private final ProductRepository productRepository;

    @Transactional
    Order createOrder(OrderDto orderDto) {
        OrderCreator orderCreator = new OrderCreator(orderDto, productRepository::findById);
        if (!orderCreator.succesfullyCreatedOrder()) {
            List<CreateNewOrderError> errors = orderCreator.getErrors();
            throw new OrderController.CreateNewOrderException(errors);
        }

        Order order = orderCreator.getOrder();
        ActiveOrderMetadata activeOrderMetadata = orderCreator.getActiveOrderMetadata();

        updateProductsInOrder(order);
        order = orderRepository.save(order);
        activeOrderMetadataRepository.save(activeOrderMetadata);
        return order;
    }

    private void updateProductsInOrder(Order order) {
        List<Product> productsToUpdate = new LinkedList<>();
        for(OrderItem orderItem : order.getItems()){
            Product product = orderItem.getProduct();
            product.reduceQuantityBy(orderItem.getQuantity());
            productsToUpdate.add(product);
        }
        productRepository.saveAll(productsToUpdate);
    }

    public Optional<Order> findById(String orderId) {
        return orderRepository.findById(orderId);
    }

    public void cancelOrder(Order order) {
        if(!order.getState().equals(Order.State.CANCELED)) {
            order.setState(Order.State.CANCELED);
            productService.returnProducts(order);
            activeOrderMetadataRepository.deleteByOrder(order);
            orderRepository.save(order);
        }
    }
}
