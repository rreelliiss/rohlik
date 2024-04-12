package com.siller.rohlik.store.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.product.ProductDto;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class SetProductTest {

    private static final String PRODUCTS_URL = "/products";
    private static final String PRODUCT_URL = "/product";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void beforeEach() {
        productRepository.deleteAll();
    }

    @Test
    public void setProductAfterPostProduct_returns204AndUpdateTheProduct() throws Exception {
        ProductDto productDto = new ProductDto()
                .name("Test Product")
                .price(new BigDecimal("13.12"))
                .quantity(5);

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String idOfSavedProduct = productRepository.findAll().get(0).getId();

        ProductDto changedProductDto = new ProductDto()
                .name("Test Product 2")
                .price(new BigDecimal("13.22"))
                .quantity(4);

        mockMvc.perform(put(PRODUCT_URL + "/" + idOfSavedProduct)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedProductDto)))
                .andDo(print())
                .andExpect(status().isNoContent());

        List<Product> allActualProducts = productRepository.findAll();
        assertEquals(1, allActualProducts.size());
        Product actualProduct = allActualProducts.get(0);

        assertEquals("Test Product 2", actualProduct.getName());
        assertEquals(13.22, actualProduct.getPrice());
        assertEquals(4, actualProduct.getQuantity());
    }

    @Test
    public void setProductWithNotExistingId_returns404() throws Exception {
        String idOfSavedProduct = "some-not-existing-id";

        ProductDto productDto = new ProductDto()
                .name("Test Product 2")
                .price(new BigDecimal("13.22"))
                .quantity(4);

        mockMvc.perform(put(PRODUCT_URL + "/" + idOfSavedProduct)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(status().isNotFound());

        List<Product> allActualProducts = productRepository.findAll();
        assertEquals(0, allActualProducts.size());
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("com.siller.rohlik.store.product.SetProductTest#inputsAndExpectedResultsOfValidation")
    public void postProduct_inputValidations(String testName, ProductDto changedProductDto, ResultMatcher matcher) throws Exception {

        ProductDto productDto = new ProductDto()
                .name("Test Product")
                .price(new BigDecimal("13.12"))
                .quantity(5);

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String idOfSavedProduct = productRepository.findAll().get(0).getId();

        mockMvc.perform(put(PRODUCT_URL + "/" + idOfSavedProduct)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedProductDto)))
                .andDo(print())
                .andExpect(matcher);

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
                        "returns NO CONTENT when name is 1 characters long ",
                        getFullyPopulatedValidProductDto().name("a"),
                        status().isNoContent()
                ),
                Arguments.of(
                        "returns NO CONTENT when name is 256 characters long ",
                        getFullyPopulatedValidProductDto().name(nChars('a', 256)),
                        status().isNoContent()
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
                        "returns NO CONTENT when price is 0 ",
                        getFullyPopulatedValidProductDto().price(new BigDecimal("0")),
                        status().isNoContent()
                ),
                Arguments.of(
                        "returns NO CONTENT when price is positive ",
                        getFullyPopulatedValidProductDto().price(new BigDecimal("10.13")),
                        status().isNoContent()
                ),
                Arguments.of(
                        "returns NO CONTENT when price is null ",
                        getFullyPopulatedValidProductDto().price(null),
                        status().isNoContent()
                ),
                Arguments.of(
                        "returns BAD REQUEST when quantity is negative",
                        getFullyPopulatedValidProductDto().quantity(-3),
                        status().isBadRequest()
                ),
                Arguments.of(
                        "returns NO CONTENT when quantity is 0 ",
                        getFullyPopulatedValidProductDto().quantity(0),
                        status().isNoContent()
                ),
                Arguments.of(
                        "returns NO CONTENT when quantity is positive ",
                        getFullyPopulatedValidProductDto().quantity(9),
                        status().isNoContent()
                ),
                Arguments.of(
                        "returns NO CONTENT when quantity is null ",
                        getFullyPopulatedValidProductDto().quantity(null),
                        status().isNoContent()
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
