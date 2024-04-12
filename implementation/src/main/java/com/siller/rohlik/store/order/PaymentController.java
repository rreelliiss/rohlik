package com.siller.rohlik.store.order;

import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.PaymentErrorCode;
import com.siller.rohlik.store.rest.api.order.OrderPaymentsApi;
import com.siller.rohlik.store.rest.model.order.CreateNewPaymentErrorResponseDto;
import com.siller.rohlik.store.rest.model.order.PaymentDto;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class PaymentController implements OrderPaymentsApi {

    private final OrderRepository orderRepository;
    private final ActiveOrderMetadataRepository activeOrderMetadataRepository;

    @Override
    @Transactional
    public ResponseEntity<Void> createPayment(String orderId, PaymentDto paymentDto) {
        Optional<Order> potentialOrder = orderRepository.findById(orderId);
        if (potentialOrder.isPresent()) {
            Order order = potentialOrder.get();
            if(order.getState().equals(Order.State.CANCELED)){
                throw new PaymentErrorException(PaymentErrorCode.CANNOT_PAY_CANCELED_ORDER);
            }
            if(order.getState().equals(Order.State.PAYED)){
                throw new PaymentErrorException(PaymentErrorCode.ALREADY_PAYED);
            }
            Optional<BigDecimal> totalPrice = order.getItems().stream()
                    .map(orderItem -> orderItem.getProduct().getPrice().multiply(new BigDecimal(orderItem.getQuantity())))
                    .reduce(BigDecimal::add);
            if(totalPrice.isPresent() && !totalPrice.get().equals(paymentDto.getAmount())){
                throw new PaymentErrorException(PaymentErrorCode.WRONG_AMOUNT);
            }
            order.setState(Order.State.PAYED);
            activeOrderMetadataRepository.deleteByOrder(order);
            orderRepository.save(order);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @ExceptionHandler(PaymentErrorException.class)
    ResponseEntity<CreateNewPaymentErrorResponseDto> handlePaymentErrorException(PaymentErrorException ex) {
        return new ResponseEntity<>(
                new CreateNewPaymentErrorResponseDto().errorCode(ex.getPaymentErrorCode().name()),
                HttpStatus.BAD_REQUEST
        );
    }

    @RequiredArgsConstructor
    @Getter
    public class PaymentErrorException extends RuntimeException {
        private final PaymentErrorCode paymentErrorCode;
    }
}
