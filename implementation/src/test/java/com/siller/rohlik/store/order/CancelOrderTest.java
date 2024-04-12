package com.siller.rohlik.store.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.order.CreateNewOrderResponseDto;
import com.siller.rohlik.store.rest.model.order.OrderDto;
import com.siller.rohlik.store.rest.model.order.OrderItemDto;
import com.siller.rohlik.store.rest.model.order.WriteableOrderStateDto;
import com.siller.rohlik.store.rest.model.product.CreateNewProductResponseDto;
import com.siller.rohlik.store.rest.model.product.ProductDto;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class CancelOrderTest {

    private static final String PRODUCTS_URL = "/products";
    private static final String ORDERS_URL = "/orders";
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
    @Transactional
    public void cancelOrder_setStateToCanceledAndReturnProductsToStock() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));

        OrderDto order2 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(2));

        String createdOrderId1 = postOrder(order1);
        postOrder(order2);

        WriteableOrderStateDto writeableOrderStateDto = new WriteableOrderStateDto().state(Order.State.CANCELED.name());
        mockMvc.perform(put(String.format(ORDERS_STATE_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeableOrderStateDto)))
                .andExpect(status().isNoContent());

        Order actualOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.CANCELED, actualOrder.getState());


        Product actualProduct1 = productRepository.findById(productId1).get();
        assertEquals(4, actualProduct1.getQuantity());
        Product actualProduct2 = productRepository.findById(productId2).get();
        assertEquals(1, actualProduct2.getQuantity());
        Product actualProduct3 = productRepository.findById(productId3).get();
        assertEquals(1, actualProduct3.getQuantity());
        Product actualProduct4 = productRepository.findById(productId4).get();
        assertEquals(0, actualProduct4.getQuantity());
    }

    @Test
    @Transactional
    public void canceledOrder_isIdempotent() throws Exception {
        OrderDto order1 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(2))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));

        OrderDto order2 = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(2));

        String createdOrderId1 = postOrder(order1);
        postOrder(order2);

        WriteableOrderStateDto writeableOrderStateDto = new WriteableOrderStateDto().state(Order.State.CANCELED.name());
        mockMvc.perform(put(String.format(ORDERS_STATE_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeableOrderStateDto)))
                .andExpect(status().isNoContent());

        mockMvc.perform(put(String.format(ORDERS_STATE_URL_TEMPLATE, createdOrderId1))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeableOrderStateDto)))
                .andExpect(status().isNoContent());

        Order actualOrder = orderRepository.findById(createdOrderId1).get();
        assertEquals(Order.State.CANCELED, actualOrder.getState());


        Product actualProduct1 = productRepository.findById(productId1).get();
        assertEquals(4, actualProduct1.getQuantity());
        Product actualProduct2 = productRepository.findById(productId2).get();
        assertEquals(1, actualProduct2.getQuantity());
        Product actualProduct3 = productRepository.findById(productId3).get();
        assertEquals(1, actualProduct3.getQuantity());
        Product actualProduct4 = productRepository.findById(productId4).get();
        assertEquals(0, actualProduct4.getQuantity());
    }

    @Test
    @Transactional
    public void cancelingNonExistingOrder_Returns404() throws Exception {

        WriteableOrderStateDto writeableOrderStateDto = new WriteableOrderStateDto().state(Order.State.CANCELED.name());
        mockMvc.perform(put(String.format(ORDERS_STATE_URL_TEMPLATE, "not-existing-order-id"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeableOrderStateDto)))
                .andExpect(status().isNotFound());

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
