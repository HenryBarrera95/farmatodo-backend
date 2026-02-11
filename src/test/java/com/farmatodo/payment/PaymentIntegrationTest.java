package com.farmatodo.payment;

import com.farmatodo.log.TransactionLogRepository;
import com.farmatodo.mail.EmailService;
import com.farmatodo.order.Order;
import com.farmatodo.order.OrderRepository;
import com.farmatodo.product.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Pago - Integración retry + recover + email")
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("farmatodo")
            .withUsername("farmatodo")
            .withPassword("farmatodo_pwd");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("ENCRYPTION_KEY", () -> "QmFja2VuZEVuY3J5cHRpb25LZXlGb3JERVZfMjU2X0dDTQ==");
        registry.add("APP_API_KEY", () -> "test-api-key");
        registry.add("payment.approve-probability", () -> "0");
        registry.add("payment.retry.max-attempts", () -> "2");
        registry.add("payment.retry.delay", () -> "50");
        registry.add("payment.retry.multiplier", () -> "1");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private TransactionLogRepository logRepo;

    @MockBean
    private EmailService emailService;

    @Test
    @DisplayName("Cuando el pago falla tras todos los reintentos: order PAYMENT_FAILED, email enviado, logs")
    void paymentFailure_afterRetries_setsOrderFailedAndSendsEmail() throws Exception {
        String customerJson = """
                {"name":"Test User","email":"test@test.com","phone":"+573001234567","address":"Calle 1 #2-3"}
                """;
        String tokenRequest = """
                {"cardNumber":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2028","cardHolderName":"JOHN DOE"}
                """;

        var createCustomer = mvc.perform(post("/clients")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createCustomer.getResponse().getHeader("Location");
        UUID customerId = UUID.fromString(location.replace("/clients/", ""));

        var tokenRes = mvc.perform(post("/tokens")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenRequest))
                .andExpect(status().isOk())
                .andReturn();
        String tokenId = JsonPath.read(tokenRes.getResponse().getContentAsString(), "$.token").toString();

        UUID productId = productRepo.findAll().get(0).getId();
        String addItemJson = """
                {"customerId":"%s","productId":"%s","quantity":1}
                """.formatted(customerId, productId);

        mvc.perform(post("/carts/items")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson))
                .andExpect(status().isCreated());

        String orderJson = """
                {"customerId":"%s","deliveryAddress":"Calle 1 #2-3","token":"%s"}
                """.formatted(customerId, tokenId);

        var orderRes = mvc.perform(post("/orders")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"))
                .andReturn();

        String responseBody = orderRes.getResponse().getContentAsString();
        UUID orderId = UUID.fromString(JsonPath.read(responseBody, "$.orderId").toString());

        Order order = orderRepo.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAYMENT_FAILED);

        verify(emailService).sendPaymentFailed(eq("test@test.com"), eq(orderId.toString()), anyString());

        assertThat(logRepo.findAll()).anyMatch(l ->
                "payment_failed".equals(l.getEventType()) && orderId.toString().equals(
                        l.getPayload() != null ? l.getPayload().get("orderId") : null));
        assertThat(logRepo.findAll()).anyMatch(l ->
                "email_sent_payment_failed".equals(l.getEventType()));
    }
}
