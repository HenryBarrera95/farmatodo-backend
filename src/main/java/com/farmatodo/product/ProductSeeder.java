package com.farmatodo.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@Profile("!test")
public class ProductSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);

    private final ProductRepository repo;

    public ProductSeeder(ProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            return;
        }
        var now = Instant.now();
        var products = List.of(
                new Product("Paracetamol 500mg", "Analgésico y antipirético", new BigDecimal("3500"), 100, now),
                new Product("Ibuprofeno 400mg", "Antiinflamatorio no esteroideo", new BigDecimal("4200"), 80, now),
                new Product("Vitamina C 1000mg", "Suplemento vitamínico", new BigDecimal("8500"), 50, now),
                new Product("Omeprazol 20mg", "Inhibidor de bomba de protones", new BigDecimal("5200"), 120, now),
                new Product("Loratadina 10mg", "Antihistamínico", new BigDecimal("3800"), 60, now),
                new Product("Amoxicilina 500mg", "Antibiótico penicilínico", new BigDecimal("12500"), 30, now),
                new Product("Dolex Gripa", "Antigripal", new BigDecimal("2800"), 5, now),
                new Product("Mascarilla quirúrgica x10", "Protección facial", new BigDecimal("15000"), 200, now)
        );
        repo.saveAll(products);
        log.info("Seeded {} products", products.size());
    }
}
