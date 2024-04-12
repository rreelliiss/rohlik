package com.siller.rohlik.store.order;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.order.model.ActiveOrderMetadata;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.order.CreateNewOrderResponseDto;
import com.siller.rohlik.store.rest.model.order.OrderDto;
import com.siller.rohlik.store.rest.model.order.OrderItemDto;
import com.siller.rohlik.store.rest.model.order.PaymentDto;
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
import java.util.List;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "inactiveOrdersProcessor.cronExpression=* * * * * *",
                "activePaymentsShouldBeInvalidatedAfterSeconds=5"
        }
)
@AutoConfigureMockMvc
public class InvalidAbandonedOrdersTest {


    private static final String PRODUCTS_URL = "/products";
    private static final String ORDERS_URL = "/orders";
    private static final String ORDERS_PAYMENT_URL_TEMPLATE = "/orders/%s/payment";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ActiveOrderMetadataRepository activeOrderMetadataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String productId1;
    private String productId2;
    private String productId3;
    private String productId4;

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

        productId4 = postProduct(
                new ProductDto()
                        .name("Test Product 4")
                        .price(new BigDecimal("7.0"))
                        .quantity(0)
        );


    }

    @Test
    public void createOrder_storesOrderAndSubtractsQuantitiesOfProducts() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));

        OrderDto order2 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(2));

        String createdOrderId1 = postOrder(order1);
        String createdOrderId2 = postOrder(order2);

        PaymentDto paymentDto = new PaymentDto().amount(new BigDecimal("30.60"));

        mockMvc.perform(put(String.format(ORDERS_PAYMENT_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getContentAsString();

        sleep(10000L);

        Order actualPayedOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.PAYED, actualPayedOrder.getState());

        Order actualNotPayedOrder = orderRepository.findById(createdOrderId2).get();
        assertEquals(Order.State.INVALIDATED, actualNotPayedOrder.getState());

        Product actualProduct1 = productRepository.findById(productId1).get();
        assertEquals(3, actualProduct1.getQuantity());
        Product actualProduct2 = productRepository.findById(productId2).get();
        assertEquals(2, actualProduct2.getQuantity());
        Product actualProduct3 = productRepository.findById(productId3).get();
        assertEquals(0, actualProduct3.getQuantity());
        Product actualProduct4 = productRepository.findById(productId4).get();
        assertEquals(0, actualProduct4.getQuantity());

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
