package com.farmatodo.token;

import com.farmatodo.TestUtils;
import com.farmatodo.token.dto.CreateTokenRequest;
import com.farmatodo.token.dto.CreateTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenController.class)
@DisplayName("TokenController")
class TokenControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TokenService tokenService;

    @Test
    @DisplayName("POST /tokens retorna 200 con token y maskedPan")
    void createToken_returns200() throws Exception {
        var tokenId = java.util.UUID.randomUUID().toString();
        var entity = new com.farmatodo.token.CardToken(
                tokenId, "ct", "iv", "tag", "**** **** **** 1111",
                java.time.Instant.now(), "tx"
        );

        when(tokenService.createToken(any(CreateTokenRequest.class))).thenReturn(entity);

        mvc.perform(post("/tokens")
                        .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardNumber":"4111111111111111","cvv":"123","expiryMonth":"12","expiryYear":"2028","cardHolderName":"JOHN DOE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(tokenId))
                .andExpect(jsonPath("$.maskedPan").value("**** **** **** 1111"));
    }

    @Test
    @DisplayName("POST /tokens con tarjeta vencida retorna 400")
    void createToken_expiredCard_returns400() throws Exception {
        mvc.perform(post("/tokens")
                        .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardNumber":"4111111111111111","cvv":"123","expiryMonth":"01","expiryYear":"2020","cardHolderName":"JOHN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"));
    }
}
