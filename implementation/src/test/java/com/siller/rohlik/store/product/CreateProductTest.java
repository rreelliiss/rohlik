package com.siller.rohlik.store.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.order.repository.ActiveOrderMetadataRepository;
import com.siller.rohlik.store.order.repository.OrderRepository;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.product.CreateNewProductResponseDto;
import com.siller.rohlik.store.rest.model.product.ProductDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static com.siller.rohlik.store.testSupport.Utils.nChars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CreateProductTest {

    private static final String PRODUCTS_URL = "/products";

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

    @AfterEach
    void cleanDb(){
        activeOrderMetadataRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    public void postProduct_returns201AndStoreTheProduct() throws Exception {
        ProductDto productDto = new ProductDto()
                .name("Test Product")
                .price(new BigDecimal("13.12"))
                .quantity(5);

        String responseAsString = mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        List<Product> allActualProducts = productRepository.findAll();
        assertEquals(1, allActualProducts.size());
        Product actualProduct = allActualProducts.get(0);

        assertEquals(responseAsString, objectMapper.writeValueAsString(new CreateNewProductResponseDto(actualProduct.getId())));
        assertEquals("Test Product", actualProduct.getName());
        assertEquals(new BigDecimal("13.12"), actualProduct.getPrice());
        assertEquals(5, actualProduct.getQuantity());
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("com.siller.rohlik.store.product.CreateProductTest#inputsAndExpectedResultsOfValidation")
    public void postProduct_inputValidations(String testName, ProductDto productDto, ResultMatcher matcher) throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(matcher)
                .andReturn().getResponse().getContentAsString();
    }

    private static Stream<Arguments> inputsAndExpectedResultsOfValidation() {


        return Stream.of(
                Arguments.of(
                        "returns BAD REQUEST when name is null",
                        getFullyPopulatedValidProductDto().name(null),
                        status().isBadRequest()
                        ),
                Arguments.of(
                        "returns BAD REQUEST when name is empty",
                        getFullyPopulatedValidProductDto().name(""),
                        status().isBadRequest()
                ),
                Arguments.of(
                        "returns CREATED when name is 1 characters long ",
                        getFullyPopulatedValidProductDto().name("a"),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns CREATED when name is 256 characters long ",
                        getFullyPopulatedValidProductDto().name(nChars('a', 256)),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns BAD_REQUEST when name is 257 characters long ",
                        getFullyPopulatedValidProductDto().name(nChars('a', 257)),
                        status().isBadRequest()
                ),
                Arguments.of(
                        "returns BAD REQUEST when price is negative",
                        getFullyPopulatedValidProductDto().price(new BigDecimal("-1.5")),
                        status().isBadRequest()
                ),
                Arguments.of(
                        "returns CREATED when price is 0 ",
                        getFullyPopulatedValidProductDto().price(new BigDecimal("0")),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns CREATED when price is positive ",
                        getFullyPopulatedValidProductDto().price(new BigDecimal("10.13")),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns CREATED when price is null ",
                        getFullyPopulatedValidProductDto().price(null),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns BAD REQUEST when quantity is negative",
                        getFullyPopulatedValidProductDto().quantity(-3),
                        status().isBadRequest()
                ),
                Arguments.of(
                        "returns CREATED when quantity is 0 ",
                        getFullyPopulatedValidProductDto().quantity(0),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns CREATED when quantity is positive ",
                        getFullyPopulatedValidProductDto().quantity(9),
                        status().isCreated()
                ),
                Arguments.of(
                        "returns CREATED when quantity is null ",
                        getFullyPopulatedValidProductDto().quantity(null),
                        status().isCreated()
                )
        );
    }

    private static ProductDto getFullyPopulatedValidProductDto() {
        ProductDto productDto = new ProductDto();
        productDto.setName("Test Product");
        productDto.setPrice(new BigDecimal("13.12"));
        productDto.setQuantity(5);
        return productDto;
    }

}