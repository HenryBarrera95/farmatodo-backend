package com.farmatodo.cart;

import com.farmatodo.TestUtils;
import com.farmatodo.cart.dto.CartItemResponse;
import com.farmatodo.cart.dto.CartResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@DisplayName("CartController")
class CartControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CartService cartService;

    @Test
    @DisplayName("POST /carts/items retorna 201")
    void addItem_returns201() throws Exception {
        var cartId = UUID.randomUUID();
        var cart = new com.farmatodo.cart.Cart(UUID.randomUUID(), com.farmatodo.cart.Cart.CartStatus.ACTIVE, java.time.Instant.now());
        TestUtils.setId(cart, cartId);

        var response = new CartResponse(cartId, cart.getCustomerId(), List.of(
                new CartItemResponse(UUID.randomUUID(), 2, BigDecimal.valueOf(100), BigDecimal.valueOf(200))
        ), BigDecimal.valueOf(200));

        when(cartService.addItem(any())).thenReturn(cart);
        when(cartService.getCart(any(UUID.class))).thenReturn(response);

        mvc.perform(post("/carts/items")
                        .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","productId":"%s","quantity":2}
                                """.formatted(cart.getCustomerId(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(200));
    }

    @Test
    @DisplayName("GET /carts?customerId= retorna carrito")
    void getCart_returns200() throws Exception {
        var customerId = UUID.randomUUID();
        var cartId = UUID.randomUUID();
        var response = new CartResponse(cartId, customerId, List.of(), BigDecimal.ZERO);
        when(cartService.getCart(customerId)).thenReturn(response);

        mvc.perform(get("/carts").param("customerId", customerId.toString()).header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }
}
