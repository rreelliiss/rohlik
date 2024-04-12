package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.CreateNewOrderError;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.rest.model.order.OrderDto;
import com.siller.rohlik.store.rest.model.order.OrderItemDto;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public class OrderCreator {

    private final OrderDto orderDto;
    private Order order;
    private ActiveOrderMetadata activeOrderMetadata;
    private final List<CreateNewOrderError> errors = new LinkedList<>();
    private final Function<String, Optional<Product>> productsSupplier;

    OrderCreator(OrderDto orderDto, Function<String, Optional<Product>> productsSupplier) {
        this.orderDto = orderDto;
        this.productsSupplier = productsSupplier;
    }

    void createOrder() {
        if (!orderIsCreated()) {
            createOrderAndMetadata();
            processOrderItems();
        }
    }

    private boolean orderIsCreated() {
        return order != null;
    }

    private void createOrderAndMetadata() {
        this.order = new Order();
        order.setState(Order.State.ACTIVE);
        this.activeOrderMetadata = new ActiveOrderMetadata(order, new Timestamp(System.currentTimeMillis()));
    }

    private void processOrderItems() {
        for (OrderItemDto orderItemDto : orderDto.getOrderItems()) {
            OrderItemCreator orderItemCreator = new OrderItemCreator(order, orderItemDto, productsSupplier);
            if(orderItemCreator.successfullyCreatedItem()){
                order.addItem(orderItemCreator.getItem());
            }
            else {
                errors.addAll(orderItemCreator.getErrors());
            }
        }
    }


    public Order getOrder() {
        createOrder();
        return order;
    }

    public List<CreateNewOrderError> getErrors() {
        createOrder();
        return errors;
    }

    public boolean succesfullyCreatedOrder() {
        createOrder();
        return errors.isEmpty() && order != null;
    }

    public ActiveOrderMetadata getActiveOrderMetadata() {
        createOrder();
        return activeOrderMetadata;
    }
}
