package com.farmatodo.product;

import com.farmatodo.config.TxFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    private final int defaultMinStock;

    public ProductService(ProductRepository repo,
                          ApplicationEventPublisher eventPublisher,
                          @Value("${app.product.min-stock-visible:0}") int defaultMinStock) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
        this.defaultMinStock = defaultMinStock;
    }

    public List<Product> search(Integer minStockParam) {
        int minStock = minStockParam != null ? minStockParam : defaultMinStock;
        String tx = MDC.get(TxFilter.TX_ID);

        eventPublisher.publishEvent(new ProductSearchEvent(this, minStock, tx != null ? tx : "unknown"));

        return repo.findByStockGreaterThanEqualOrderByNameAsc(minStock);
    }
}
