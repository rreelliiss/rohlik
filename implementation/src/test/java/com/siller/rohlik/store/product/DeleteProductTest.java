package com.siller.rohlik.store.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siller.rohlik.store.product.model.Product;
import com.siller.rohlik.store.product.repository.ProductRepository;
import com.siller.rohlik.store.rest.model.CreateNewProductResponseDto;
import com.siller.rohlik.store.rest.model.ProductDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DeleteProductTest {

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
    public void deleteProductAfterPostProduct_returns204AndDeleteTheProduct() throws Exception {

        ProductDto productDto = new ProductDto()
                .name("Test Product")
                .price(new BigDecimal("13.12"))
                .quantity(5);

        ProductDto anotherProductDto = new ProductDto()
                .name("another Test Product");


        String responseFromSavingProduct = mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String responseFromSavingAnotherProduct = mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anotherProductDto)))
                .andDo(print())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String idOfSavedProduct = objectMapper.readValue(responseFromSavingProduct, CreateNewProductResponseDto.class).getId();
        String idOfSavedAnotherProduct = objectMapper.readValue(responseFromSavingAnotherProduct, CreateNewProductResponseDto.class).getId();

        mockMvc.perform(delete(PRODUCT_URL + "/" + idOfSavedProduct))
                .andDo(print())
                .andExpect(status().isNoContent());

        List<Product> allActualProducts = productRepository.findAll();
        Product actualProduct = allActualProducts.get(0);
        assertEquals(1, allActualProducts.size());
        assertEquals("another Test Product", actualProduct.getName());
        assertEquals(idOfSavedAnotherProduct, actualProduct.getId());
    }

    @Test
    public void deleteProductWithNotExistingId_returns204AsTheProductIsDeleted() throws Exception {

        ProductDto productDto = new ProductDto()
                .name("Test Product")
                .price(new BigDecimal("13.12"))
                .quantity(5);

        String responseFromSavingProduct = mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andDo(print())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String idOfSavedProduct = objectMapper.readValue(responseFromSavingProduct, CreateNewProductResponseDto.class).getId();

        mockMvc.perform(delete(PRODUCT_URL + "/not-existing-id"))
                .andDo(print())
                .andExpect(status().isNoContent());

        List<Product> allActualProducts = productRepository.findAll();
        Product actualProduct = allActualProducts.get(0);
        assertEquals(1, allActualProducts.size());
        assertEquals("Test Product", actualProduct.getName());
        assertEquals(idOfSavedProduct, actualProduct.getId());
    }
}
