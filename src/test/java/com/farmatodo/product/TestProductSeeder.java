package com.farmatodo.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Configuration
@Profile("test")
public class TestProductSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            var now = Instant.now();
            var products = List.of(
                    new Product("Paracetamol 500mg", "Analgésico", new BigDecimal("3500"), 100, now),
                    new Product("Ibuprofeno 400mg", "Antiinflamatorio", new BigDecimal("4200"), 80, now)
            );
            repo.saveAll(products);
        };
    }
}
