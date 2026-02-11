package com.farmatodo.log;

import com.farmatodo.config.TxFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogService")
class LogServiceTest {

    @Mock
    private TransactionLogRepository repository;

    private LogService service;

    @BeforeEach
    void setUp() {
        service = new LogService(repository);
        MDC.put("tx_id", "test-tx-123");
    }

    @Test
    @DisplayName("log con payload persiste TransactionLog")
    void log_withPayload() {
        service.log("order_created", "INFO", "Order created", Map.of("orderId", "abc"));

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(repository).save(captor.capture());

        TransactionLog log = captor.getValue();
        assertThat(log.getTxId()).isEqualTo("test-tx-123");
        assertThat(log.getEventType()).isEqualTo("order_created");
        assertThat(log.getLevel()).isEqualTo("INFO");
        assertThat(log.getMessage()).isEqualTo("Order created");
        assertThat(log.getPayload()).containsEntry("orderId", "abc");
    }

    @Test
    @DisplayName("log sin payload usa null")
    void log_withoutPayload() {
        service.log("event", "WARN", "msg");

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).isNull();
    }
}
