package com.farmatodo.log;

import com.farmatodo.config.TxFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class LogService {

    private final TransactionLogRepository repository;

    public LogService(TransactionLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String level, String message, Map<String, Object> payload) {
        String txId = MDC.get(TxFilter.TX_ID);
        if (txId == null) {
            txId = "unknown";
        }
        TransactionLog log = new TransactionLog(txId, eventType, level, message, payload, Instant.now());
        repository.save(log);
    }

    public void log(String eventType, String level, String message) {
        log(eventType, level, message, null);
    }
}
