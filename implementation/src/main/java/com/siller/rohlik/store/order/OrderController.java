package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.CreateNewOrderError;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.rest.api.order.OrdersApi;
import com.siller.rohlik.store.rest.model.order.*;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrdersApi {


    private final OrderService orderService;


    @Override
    @Transactional
    public ResponseEntity<CreateNewOrderResponseDto> createNewOrder(OrderDto orderDto) {
        Order order = orderService.createOrder(orderDto);
        return new ResponseEntity<>(new CreateNewOrderResponseDto(order.getId()), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> setOrderState(String orderId, WriteableOrderStateDto writeableOrderStateDto) {
        validateRequestedState(writeableOrderStateDto);
        Optional<Order> optionalOrder = orderService.findById(orderId);
        if (optionalOrder.isPresent()) {
            orderService.cancelOrder(optionalOrder.get());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private static void validateRequestedState(WriteableOrderStateDto writeableOrderStateDto) {
        if (!writeableOrderStateDto.getState().equals(Order.State.CANCELED.name())) {
            throw new ValidationException("Only allowed state to write is " + Order.State.CANCELED);
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
