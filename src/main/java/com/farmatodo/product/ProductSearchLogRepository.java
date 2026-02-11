package com.farmatodo.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductSearchLogRepository extends JpaRepository<ProductSearchLog, UUID> {
}
