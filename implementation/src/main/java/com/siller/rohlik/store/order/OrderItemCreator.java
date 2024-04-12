package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.CreateNewOrderError;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.OrderItem;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.rest.model.order.OrderItemDto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public class OrderItemCreator {

    private final Order order;
    private final List<CreateNewOrderError> errors = new LinkedList<>();
    private final OrderItemDto orderItemDto;
    private final Function<String, Optional<Product>> productsSupplier;
    private Product product;
    private OrderItem orderItem;

    public OrderItemCreator(Order order, OrderItemDto orderItemDto, Function<String, Optional<Product>> productsSupplier) {
        this.order = order;
        this.orderItemDto = orderItemDto;
        this.productsSupplier = productsSupplier;;
    }

    private void process() {
        if (itemNotProcessedYet()) {
            if (orderItemDto.getQuantity() == null) {
                errors.add(missingQuantityError());
                return;
            }
            Optional<Product> potentialProduct = getPotentialProduct();
            if (potentialProduct.isEmpty()) {
                errors.add(invalidProductError());
                return;
            }
            product = potentialProduct.get();
            if (product.isNotFinished()) {
                errors.add(unfinishedProductError());
                return;
            }
            if (product.doesNotHaveEnoughQuantity(orderItemDto.getQuantity())) {
                errors.add(notEnoughProductsOnStockError());
                return;
            }
            createOrderItem();
        }
    }

    private Optional<Product> getPotentialProduct() {
        return productsSupplier.apply(orderItemDto.getProductId());
    }

    private boolean itemNotProcessedYet() {
        return orderItem == null && errors.isEmpty();
    }

    private CreateNewOrderError invalidProductError() {
        return new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.INVALID_PRODUCT);
    }

    private CreateNewOrderError notEnoughProductsOnStockError() {
        return new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.NOT_ENOUGH_PRODUCTS_ON_STOCK);
    }

    private CreateNewOrderError unfinishedProductError() {
        return new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.UNFINISHED_PRODUCT);
    }

    private CreateNewOrderError missingQuantityError() {
        return new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.MISSING_QUANTITY);
    }

    private void createOrderItem() {
        orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(orderItemDto.getQuantity());
        orderItem.setOrder(order);
    }

    public OrderItem getItem() {
        process();
        return orderItem;
    }

    public Collection<CreateNewOrderError> getErrors() {
        process();
        return errors;
    }

    public boolean successfullyCreatedItem() {
        process();
        return errors.isEmpty() && orderItem != null;
    }
}
