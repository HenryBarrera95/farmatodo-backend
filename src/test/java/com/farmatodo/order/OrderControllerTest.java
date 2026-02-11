package com.farmatodo.order;

import com.farmatodo.TestUtils;
import com.farmatodo.order.dto.CreateOrderRequest;
import com.farmatodo.order.dto.OrderResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /orders retorna 201 y Location")
    void create_returns201() throws Exception {
        var req = new CreateOrderRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setDeliveryAddress("Calle 1 #2-3");
        req.setToken(UUID.randomUUID().toString());

        var order = new com.farmatodo.order.Order(
                req.getCustomerId(), UUID.randomUUID(),
                com.farmatodo.order.Order.OrderStatus.PAYMENT_PENDING,
                BigDecimal.valueOf(100), "Calle 1 #2-3", req.getToken(),
                java.time.Instant.now(), "tx"
        );
        TestUtils.setId(order);

        var response = new OrderResponse(
                order.getId(), order.getCustomerId(), order.getCartId(),
                order.getStatus(), List.of(), order.getTotalAmount(),
                order.getDeliveryAddress(), order.getTokenId()
        );

        when(orderService.create(any())).thenReturn(order);
        when(orderService.toResponse(any())).thenReturn(response);

        mvc.perform(post("/orders")
                        .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","deliveryAddress":"Calle 1 #2-3","token":"%s"}
                                """.formatted(req.getCustomerId(), req.getToken())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/orders/")))
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
    }
}
