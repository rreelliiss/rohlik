package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.CreateNewOrderError;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.OrderItem;
import com.siller.rohlik.store.product.ProductService;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.api.order.OrdersApi;
import com.siller.rohlik.store.rest.model.order.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrdersApi {

    private final OrderRepository orderRepository;
    private final ActiveOrderMetadataRepository activeOrderMetadataRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Override
    @Transactional
    public ResponseEntity<CreateNewOrderResponseDto> createNewOrder(OrderDto orderDto) {

        Order order = new Order();
        order.setState(Order.State.ACTIVE);
        List<Product> productsToUpdate = new LinkedList<>();
        List<CreateNewOrderError> errors = new LinkedList<>();
        for (OrderItemDto orderItemDto : orderDto.getOrderItems()) {
            if(orderItemDto.getQuantity() == null) {
                errors.add(new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.MISSING_QUANTITY));
                continue;
            }
            OrderItem orderItem = new OrderItem();
            Optional<Product> potentialProduct = productRepository.findById(orderItemDto.getProductId());
            if (potentialProduct.isPresent()) {
                Product product = potentialProduct.get();
                if(product.isNotFinished()){
                    errors.add(new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.UNFINISHED_PRODUCT));
                    continue;
                }
                if (product.doesNotHaveEnoughQuantity(orderItemDto.getQuantity())) {
                    errors.add(new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.NOT_ENOUGH_PRODUCTS_ON_STOCK));
                    continue;
                }
                orderItem.setProduct(product);
                orderItem.setQuantity(orderItemDto.getQuantity());
                orderItem.setOrder(order);
                order.getItems().add(orderItem);
                product.setQuantity(product.getQuantity() - orderItem.getQuantity());
                productsToUpdate.add(product);
            } else {
                errors.add(new CreateNewOrderError(orderItemDto.getProductId(), CreateNewOrderError.Code.INVALID_PRODUCT));
            }

        }
        if (!errors.isEmpty()) {
            throw new CreateNewOrderException(errors);
        }

        ActiveOrderMetadata activeOrderMetadata = new ActiveOrderMetadata(order, new Timestamp(System.currentTimeMillis()));
        productRepository.saveAll(productsToUpdate);
        order = orderRepository.save(order);
        activeOrderMetadataRepository.save(activeOrderMetadata);

        return new ResponseEntity<>(new CreateNewOrderResponseDto(order.getId()), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> setOrderState(String orderId, WriteableOrderStateDto writeableOrderStateDto) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            if(!order.getState().equals(Order.State.CANCELED)) {
                order.setState(Order.State.CANCELED);
                productService.returnProducts(order);
                activeOrderMetadataRepository.deleteByOrder(order);
                orderRepository.save(order);
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @ExceptionHandler(CreateNewOrderException.class)
    ResponseEntity<CreateNewOrderErrorResponseDto> handleException(CreateNewOrderException createNewOrderException) {
        List<CreateNewOrderErrorDto> errorDtos = createNewOrderException.getErrors().stream()
                .map(error -> new CreateNewOrderErrorDto()
                        .errorCode(error.getErrorCode().name())
                        .productId(error.getProductId()))
                .toList();
        return new ResponseEntity<>(
                new CreateNewOrderErrorResponseDto().errors(errorDtos),
                HttpStatus.BAD_REQUEST
        );
    }

    public static class CreateNewOrderException extends RuntimeException {

        @Getter
        private final List<CreateNewOrderError> errors;

        public CreateNewOrderException(List<CreateNewOrderError> errors) {
            super("Error creating new order");
            this.errors = errors;
        }
    }
}
