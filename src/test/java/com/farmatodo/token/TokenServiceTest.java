package com.farmatodo.token;

import com.farmatodo.crypto.EncryptionResult;
import com.farmatodo.crypto.EncryptionService;
import com.farmatodo.log.LogService;
import com.farmatodo.token.dto.CreateTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService")
class TokenServiceTest {

    private static final byte[] MOCK_CIPHERTEXT = "mock-ct".getBytes();
    private static final byte[] MOCK_IV = new byte[12];
    private static final byte[] MOCK_AUTH_TAG = new byte[16];

    @Mock
    private CardTokenRepository repo;

    @Mock
    private LogService logService;

    @Mock
    private EncryptionService encryptionService;

    private CreateTokenRequest validRequest() {
        var req = new CreateTokenRequest();
        req.setCardNumber("4111111111111111");
        req.setCvv("123");
        req.setExpiryMonth("12");
        req.setExpiryYear("2028");
        req.setCardHolderName("JOHN DOE");
        return req;
    }

    @Nested
    @DisplayName("caso éxito (rejectProbability = 0)")
    class SuccessCase {

        private TokenService service;

        @BeforeEach
        void setUp() {
            service = new TokenService(repo, logService, encryptionService, 0.0);
            when(encryptionService.encrypt(anyString()))
                    .thenReturn(new EncryptionResult(MOCK_CIPHERTEXT, MOCK_IV, MOCK_AUTH_TAG));
        }

        @Test
        @DisplayName("token no null y maskedPan correcto")
        void returnsTokenAndCorrectMaskedPan() {
            MDC.put("tx_id", "test-tx-1");
            try {
                CardToken result = service.createToken(validRequest());

                assertThat(result.getToken()).isNotNull().matches(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
                assertThat(result.getMaskedPan()).isEqualTo("**** **** **** 1111");
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("repository.save fue llamado")
        void repositorySaveCalled() {
            MDC.put("tx_id", "test-tx-2");
            try {
                service.createToken(validRequest());
                verify(repo).save(any(CardToken.class));
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("logService.log token_created fue llamado")
        void logServiceTokenCreatedCalled() {
            MDC.put("tx_id", "test-tx-3");
            try {
                service.createToken(validRequest());

                ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> level = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);

                verify(logService).log(eventType.capture(), level.capture(), message.capture(), payload.capture());

                assertThat(eventType.getValue()).isEqualTo("token_created");
                assertThat(level.getValue()).isEqualTo("INFO");
                assertThat(payload.getValue()).containsKeys("token", "maskedPan");
                assertThat(payload.getValue().get("maskedPan")).isEqualTo("**** **** **** 1111");
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("entity guardado tiene tx_id del MDC")
        void savedEntityHasTxIdFromMdc() {
            MDC.put("tx_id", "my-trace-id-123");
            try {
                ArgumentCaptor<CardToken> captor = ArgumentCaptor.forClass(CardToken.class);
                service.createToken(validRequest());
                verify(repo).save(captor.capture());

                assertThat(captor.getValue().getTxId()).isEqualTo("my-trace-id-123");
            } finally {
                MDC.clear();
            }
        }
    }

    @Nested
    @DisplayName("caso rechazo (rejectProbability = 1)")
    class RejectionCase {

        private TokenService service;

        @BeforeEach
        void setUp() {
            service = new TokenService(repo, logService, encryptionService, 1.0);
        }

        @Test
        @DisplayName("lanza TokenRejectedException")
        void throwsTokenRejectedException() {
            MDC.put("tx_id", "test-tx-reject");
            try {
                assertThatThrownBy(() -> service.createToken(validRequest()))
                        .isInstanceOf(TokenRejectedException.class)
                        .hasMessageContaining("Tokenization rejected");
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("repository.save NO fue llamado")
        void repositorySaveNotCalled() {
            MDC.put("tx_id", "test-tx-reject");
            try {
                assertThatThrownBy(() -> service.createToken(validRequest()))
                        .isInstanceOf(TokenRejectedException.class);
                verify(repo, never()).save(any());
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("encryptionService.encrypt NO fue llamado")
        void encryptionServiceNotCalled() {
            MDC.put("tx_id", "test-tx-reject");
            try {
                assertThatThrownBy(() -> service.createToken(validRequest()))
                        .isInstanceOf(TokenRejectedException.class);
                verify(encryptionService, never()).encrypt(anyString());
            } finally {
                MDC.clear();
            }
        }

        @Test
        @DisplayName("logService.log token_rejected fue llamado")
        void logServiceTokenRejectedCalled() {
            MDC.put("tx_id", "test-tx-reject");
            try {
                assertThatThrownBy(() -> service.createToken(validRequest()))
                        .isInstanceOf(TokenRejectedException.class);

                verify(logService).log(
                        eq("token_rejected"),
                        eq("WARN"),
                        eq("Tokenization rejected by configured probability"),
                        any(Map.class));
            } finally {
                MDC.clear();
            }
        }
    }

    @Nested
    @DisplayName("maskedPan correcto")
    class MaskedPanContract {

        @Test
        @DisplayName("4111111111111111 -> **** **** **** 1111")
        void masksCorrectly() {
            TokenService service = new TokenService(repo, logService, encryptionService, 0.0);
            when(encryptionService.encrypt(anyString()))
                    .thenReturn(new EncryptionResult(MOCK_CIPHERTEXT, MOCK_IV, MOCK_AUTH_TAG));

            MDC.put("tx_id", "test-tx-mask");
            try {
                var req = validRequest();
                req.setCardNumber("4111111111111111");

                CardToken result = service.createToken(req);

                assertThat(result.getMaskedPan()).isEqualTo("**** **** **** 1111");
            } finally {
                MDC.clear();
            }
        }
    }
}
