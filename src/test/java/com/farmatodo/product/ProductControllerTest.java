package com.farmatodo.product;

import com.farmatodo.TestUtils;
import com.farmatodo.product.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("GET /products retorna lista")
    void search_returnsList() throws Exception {
        var p = new Product("Prod", "Desc", BigDecimal.TEN, 5, Instant.now());
        TestUtils.setId(p);

        when(productService.search(null)).thenReturn(List.of(p));

        mvc.perform(get("/products").header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Prod"))
                .andExpect(jsonPath("$[0].stock").value(5));
    }

    @Test
    @DisplayName("GET /products?minStock=1 filtra por stock")
    void search_withMinStock() throws Exception {
        when(productService.search(1)).thenReturn(List.of());

        mvc.perform(get("/products").param("minStock", "1").header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
