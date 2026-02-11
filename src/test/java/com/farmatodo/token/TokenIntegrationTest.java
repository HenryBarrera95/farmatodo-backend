package com.farmatodo.token;

import com.farmatodo.log.TransactionLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Tokenización - Integración")
class TokenIntegrationTest {

    private static final String VALID_TOKEN_REQUEST = """
            {
              "cardNumber": "4111111111111111",
              "cvv": "123",
              "expiryMonth": "12",
              "expiryYear": "2028",
              "cardHolderName": "JOHN DOE"
            }
            """;

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
        registry.add("APP_API_KEY", () -> "test-api-key-integration");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CardTokenRepository cardTokenRepo;

    @Autowired
    private TransactionLogRepository transactionLogRepo;

    @Test
    @DisplayName("POST /tokens retorna 200, persiste en card_tokens y transaction_logs")
    void createToken_persistsAndReturns200() throws Exception {
        var response = mvc.perform(post("/tokens")
                        .header("X-API-KEY", "test-api-key-integration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOKEN_REQUEST))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Transaction-Id"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.maskedPan").value("**** **** **** 1111"))
                .andReturn();

        String txId = response.getResponse().getHeader("X-Transaction-Id");
        assertThat(txId).isNotBlank();

        var savedTokens = cardTokenRepo.findAll();
        assertThat(savedTokens).hasSize(1);
        assertThat(savedTokens.get(0).getMaskedPan()).isEqualTo("**** **** **** 1111");
        assertThat(savedTokens.get(0).getTxId()).isEqualTo(txId);

        var logs = transactionLogRepo.findAll();
        assertThat(logs).anyMatch(l -> "token_created".equals(l.getEventType()) && txId.equals(l.getTxId()));
    }
}
