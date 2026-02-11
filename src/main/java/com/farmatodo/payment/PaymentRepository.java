package com.farmatodo.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.orderId = :orderId")
    long countByOrderId(UUID orderId);
}
