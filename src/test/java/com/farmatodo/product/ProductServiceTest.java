package com.farmatodo.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository repo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repo, eventPublisher, 0);
        MDC.put("tx_id", "test-tx");
    }

    @Test
    @DisplayName("search publica ProductSearchEvent y devuelve productos del repo")
    void search_publishesEventAndReturnsProducts() {
        var products = List.of(
                new Product("Prod1", "Desc", BigDecimal.TEN, 5, Instant.now())
        );
        when(repo.findByStockGreaterThanEqualOrderByNameAsc(0)).thenReturn(products);

        var result = service.search(null);

        assertThat(result).hasSize(1).element(0).satisfies(p -> {
            assertThat(p.getName()).isEqualTo("Prod1");
            assertThat(p.getStock()).isEqualTo(5);
        });
        verify(repo).findByStockGreaterThanEqualOrderByNameAsc(0);

        var captor = ArgumentCaptor.forClass(ProductSearchEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getMinStock()).isEqualTo(0);
        assertThat(captor.getValue().getTxId()).isEqualTo("test-tx");
    }

    @Test
    @DisplayName("search con minStock usa el parámetro")
    void search_withMinStock() {
        when(repo.findByStockGreaterThanEqualOrderByNameAsc(10)).thenReturn(List.of());

        var result = service.search(10);

        assertThat(result).isEmpty();
        verify(repo).findByStockGreaterThanEqualOrderByNameAsc(10);
    }
}
