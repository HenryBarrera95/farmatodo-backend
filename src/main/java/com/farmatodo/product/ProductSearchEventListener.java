package com.farmatodo.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ProductSearchEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchEventListener.class);

    private final ProductSearchLogRepository repository;

    public ProductSearchEventListener(ProductSearchLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Logs search intention asynchronously. Runs in a different thread (no MDC propagation).
     * Uses txId from event payload. Represents intent to search, not necessarily successful query.
     */
    @Async
    @EventListener
    public void onProductSearch(ProductSearchEvent event) {
        try {
            var logEntry = new ProductSearchLog(
                    event.getMinStock(),
                    event.getTxId(),
                    Instant.now()
            );
            repository.save(logEntry);
            log.debug("Product search logged [minStock={}, tx={}]", event.getMinStock(), event.getTxId());
        } catch (Exception e) {
            log.warn("Failed to persist product search log [tx={}]", event.getTxId(), e);
        }
    }
}
