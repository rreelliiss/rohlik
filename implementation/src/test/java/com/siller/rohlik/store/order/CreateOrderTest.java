package com.siller.rohlik.store.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.order.model.CreateNewOrderError;
import com.siller.rohlik.store.order.model.Order;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.order.CreateNewOrderErrorResponseDto;
import com.siller.rohlik.store.rest.model.order.CreateNewOrderResponseDto;
import com.siller.rohlik.store.rest.model.order.OrderDto;
import com.siller.rohlik.store.rest.model.order.OrderItemDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CreateOrderTest {

    private static final String PRODUCTS_URL = "/products";
    private static final String ORDERS_URL = "/orders";

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
    private String productWithoutPriceId;

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

        productWithoutPriceId = postProduct(
                new ProductDto()
                        .name("Test Product 5")
                        .quantity(5)
        );
    }

    @Test
    @Transactional
    public void createOrder_storesOrderAndSubtractsQuantitiesOfProducts() throws Exception {
        OrderDto orderDto = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(3))
                .addOrderItemsItem(new OrderItemDto().productId(productId2).quantity(1))
                .addOrderItemsItem(new OrderItemDto().productId(productId3).quantity(1));

        String responseFromSavingOrder = mockMvc.perform(post(ORDERS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String createdOrderId = objectMapper.readValue(responseFromSavingOrder, CreateNewOrderResponseDto.class).getId();

        List<Order> actualOrders = orderRepository.findAll();
        Order actualOrder = actualOrders.get(0);
        assertEquals(1, actualOrders.size());

        assertEquals(createdOrderId, actualOrder.getId());
        assertEquals(Order.State.ACTIVE, actualOrder.getState());

        assertEquals(3, actualOrder.getItems().size());
        assertEquals(productId1, actualOrder.getItems().get(0).getProduct().getId());
        assertEquals(2, actualOrder.getItems().get(0).getProduct().getQuantity());
        assertEquals(3, actualOrder.getItems().get(0).getQuantity());

        assertEquals(productId2, actualOrder.getItems().get(1).getProduct().getId());
        assertEquals(2, actualOrder.getItems().get(1).getProduct().getQuantity());
        assertEquals(1, actualOrder.getItems().get(1).getQuantity());

        assertEquals(productId3, actualOrder.getItems().get(2).getProduct().getId());
        assertEquals(0, actualOrder.getItems().get(2).getProduct().getQuantity());
        assertEquals(1, actualOrder.getItems().get(2).getQuantity());

        Product actualProduct1 = productRepository.findById(productId1).get();
        assertEquals(2, actualProduct1.getQuantity());

        Product actualProduct2 = productRepository.findById(productId2).get();
        assertEquals(2, actualProduct2.getQuantity());

        Product actualProduct3 = productRepository.findById(productId3).get();
        assertEquals(0, actualProduct3.getQuantity());
        Product actualProduct4 = productRepository.findById(productId4).get();
        assertEquals(0, actualProduct4.getQuantity());
    }

    @Test
    public void createOrder_withInvalidOrderItems_returnsBadRequest() throws Exception {
        OrderDto orderDto = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().productId("invalidProductId").quantity(3))
                .addOrderItemsItem(new OrderItemDto().productId(productId1).quantity(75))
                .addOrderItemsItem(new OrderItemDto().productId(productId2))
                .addOrderItemsItem(new OrderItemDto().productId(productWithoutPriceId).quantity(2));

        String errorResponseFromSavingOrder = mockMvc.perform(post(ORDERS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        CreateNewOrderErrorResponseDto errorResponse = objectMapper.readValue(errorResponseFromSavingOrder, CreateNewOrderErrorResponseDto.class);

        assertEquals(4, errorResponse.getErrors().size());

        assertEquals("invalidProductId", errorResponse.getErrors().get(0).getProductId());
        assertEquals(CreateNewOrderError.Code.INVALID_PRODUCT.name(), errorResponse.getErrors().get(0).getErrorCode());

        assertEquals(productId1, errorResponse.getErrors().get(1).getProductId());
        assertEquals(CreateNewOrderError.Code.NOT_ENOUGH_PRODUCTS_ON_STOCK.name(), errorResponse.getErrors().get(1).getErrorCode());

        assertEquals(productId2, errorResponse.getErrors().get(2).getProductId());
        assertEquals(CreateNewOrderError.Code.MISSING_QUANTITY.name(), errorResponse.getErrors().get(2).getErrorCode());

        assertEquals(productWithoutPriceId, errorResponse.getErrors().get(3).getProductId());
        assertEquals(CreateNewOrderError.Code.UNFINISHED_PRODUCT.name(), errorResponse.getErrors().get(3).getErrorCode());
    }



    @Test
    public void createOrder_withMissingProductId_returnsBadRequest() throws Exception {
        OrderDto orderDto = new OrderDto()
                .addOrderItemsItem(new OrderItemDto().quantity(8));

        mockMvc.perform(post(ORDERS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isBadRequest());

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
