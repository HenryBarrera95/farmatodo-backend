package com.farmatodo.exception;

import com.farmatodo.TestUtils;
import com.farmatodo.cart.CartController;
import com.farmatodo.cart.CartException;
import com.farmatodo.cart.CartService;
import com.farmatodo.client.CustomerConflictException;
import com.farmatodo.client.CustomerController;
import com.farmatodo.client.CustomerService;
import com.farmatodo.order.OrderController;
import com.farmatodo.order.OrderException;
import com.farmatodo.order.OrderNotFoundException;
import com.farmatodo.order.OrderService;
import com.farmatodo.token.TokenController;
import com.farmatodo.token.TokenRejectedException;
import com.farmatodo.token.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {TokenController.class, OrderController.class, CartController.class, CustomerController.class})
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    @MockBean
    private CustomerService customerService;

    @Nested
    @DisplayName("TokenRejectedException -> 422")
    class TokenRejected {
        @Test
        void returnsUnprocessableEntity() throws Exception {
            when(tokenService.createToken(any())).thenThrow(new TokenRejectedException("Rejected"));

            mvc.perform(post("/tokens")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cardNumber":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2028","cardHolderName":"JOHN"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Token Rejected"))
                    .andExpect(jsonPath("$.detail").value("Rejected"));
        }
    }

    @Nested
    @DisplayName("OrderNotFoundException -> 404")
    class OrderNotFound {
        @Test
        void returnsNotFound() throws Exception {
            when(orderService.create(any())).thenThrow(new OrderNotFoundException(java.util.UUID.randomUUID()));

            mvc.perform(post("/orders")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"customerId":"00000000-0000-0000-0000-000000000001","deliveryAddress":"Addr","token":"tok"}
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Not Found"))
                    .andExpect(jsonPath("$.detail", containsString("Order not found")));
        }
    }

    @Nested
    @DisplayName("OrderException -> 400")
    class OrderError {
        @Test
        void returnsBadRequest() throws Exception {
            when(orderService.create(any())).thenThrow(new OrderException("Cart is empty"));

            mvc.perform(post("/orders")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"customerId":"00000000-0000-0000-0000-000000000001","deliveryAddress":"Addr","token":"tok"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Order Error"))
                    .andExpect(jsonPath("$.detail").value("Cart is empty"));
        }
    }

    @Nested
    @DisplayName("CartException -> 400")
    class CartError {
        @Test
        void returnsBadRequest() throws Exception {
            when(cartService.addItem(any())).thenThrow(new CartException("Product not found"));

            mvc.perform(post("/carts/items")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"customerId":"00000000-0000-0000-0000-000000000001","productId":"00000000-0000-0000-0000-000000000002","quantity":1}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cart Error"));
        }
    }

    @Nested
    @DisplayName("CustomerConflictException -> 409")
    class CustomerConflict {
        @Test
        void returnsConflict() throws Exception {
            when(customerService.create(any())).thenThrow(new CustomerConflictException("Email already registered"));

            mvc.perform(post("/clients")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"A","email":"a@a.com","phone":"+571","address":"A"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Conflict"));
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException -> 400")
    class Validation {
        @Test
        void returnsBadRequest() throws Exception {
            mvc.perform(post("/clients")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation Failed"));
        }
    }

    @Nested
    @DisplayName("Generic Exception -> 500")
    class Generic {
        @Test
        void returnsInternalServerError() throws Exception {
            when(customerService.create(any())).thenThrow(new RuntimeException("Unexpected"));

            mvc.perform(post("/clients")
                            .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"A","email":"a@a.com","phone":"+571","address":"A"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal Server Error"));
        }
    }
}
