package com.siller.rohlik.store.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.order.model.PaymentErrorCode;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.order.*;
import com.siller.rohlik.store.rest.model.product.CreateNewProductResponseDto;
import com.siller.rohlik.store.rest.model.product.ProductDto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class OrderPaymentTest {

    private static final String PRODUCTS_URL = "/products";
    private static final String ORDERS_URL = "/orders";
    private static final String ORDERS_PAYMENT_URL_TEMPLATE = "/orders/%s/payment";
    private static final String ORDERS_STATE_URL_TEMPLATE = "/orders/%s/state";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String productId1;
    private String productId2;
    private String productId3;

    @BeforeEach
    void beforeEach() throws Exception {
        productRepository.deleteAll();
        productId1 = postProduct(
                new ProductDto()
                        .name("Test Product 1")
                        .price(new BigDecimal("13.12"))
                        .quantity(5)
        );

        productId2 = postProduct(
                new ProductDto()
                        .name("Test Product 2")
                        .price(new BigDecimal("3.24"))
                        .quantity(3)
        );

        productId3 = postProduct(
                new ProductDto()
                        .name("Test Product 3")
                        .price(new BigDecimal("1.12"))
                        .quantity(1)
        );

    }

    @Test
    @Transactional
    public void paymentOfOrder_setStateToPayed() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));

        String createdOrderId1 = postOrder(order1);

        PaymentDto paymentDto = new PaymentDto().amount(new BigDecimal("30.60"));
        mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isNoContent());

        Order actualOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.PAYED, actualOrder.getState());
    }


    @Test
    @Transactional
    public void paymentOfNotExistingOrder_returnsNotFound() throws Exception {

        PaymentDto paymentDto = new PaymentDto().amount(new BigDecimal("30.60"));
        mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, "not-existing-id"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isNotFound());

    }

    @Test
    @Transactional
    public void paymentOfOrder_withWrongAmount_isNotAllowed() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));

        String createdOrderId1 = postOrder(order1);

        PaymentDto paymentDto = new PaymentDto().amount(new BigDecimal("1.60"));
        String paymentResponse = mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        CreateNewPaymentErrorResponseDto errorResponse = objectMapper.readValue(paymentResponse, CreateNewPaymentErrorResponseDto.class);

        assertEquals(PaymentErrorCode.WRONG_AMOUNT.name(), errorResponse.getErrorCode());

        Order actualOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.ACTIVE, actualOrder.getState());
    }

    @Test
    @Transactional
    public void paymentOfCanceledOrder_isNotAllowed() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));


        String createdOrderId1 = postOrder(order1);
        cancelOrder(createdOrderId1);

        PaymentDto paymentDto = new PaymentDto().amount(new BigDecimal("30.60"));
        String paymentResponse = mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        CreateNewPaymentErrorResponseDto errorResponse = objectMapper.readValue(paymentResponse, CreateNewPaymentErrorResponseDto.class);

        assertEquals(PaymentErrorCode.CANNOT_PAY_CANCELED_ORDER.name(), errorResponse.getErrorCode());

        Order actualOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.CANCELED, actualOrder.getState());
    }

    @Test
    @Transactional
    public void paymentOfPayedOrder_isNotAllowed() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));


        String createdOrderId1 = postOrder(order1);

        PaymentDto paymentDto = new PaymentDto().amount(new BigDecimal("30.60"));
        mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getContentAsString();

        String paymentResponse = mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        CreateNewPaymentErrorResponseDto errorResponse = objectMapper.readValue(paymentResponse, CreateNewPaymentErrorResponseDto.class);

        assertEquals(PaymentErrorCode.ALREADY_PAYED.name(), errorResponse.getErrorCode());

        Order actualOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.PAYED, actualOrder.getState());
    }

    private void cancelOrder(String createdOrderId1) throws Exception {
        WriteableOrderStateDto writeableOrderStateDto = new WriteableOrderStateDto().state(Order.State.CANCELED.name());
        mockMvc.perform(put(String.format(ORDERS_STATE_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeableOrderStateDto)))
                .andExpect(status().isNoContent());
    }


    private String postOrder(OrderDto orderDto) throws Exception {
        String responseFromSavingOrder = mockMvc.perform(post(ORDERS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseFromSavingOrder, CreateNewOrderResponseDto.class).getId();
    }


    private String postProduct(ProductDto product) throws Exception {
        String responseFromSavingProduct = mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseFromSavingProduct, CreateNewProductResponseDto.class).getId();
    }

}
