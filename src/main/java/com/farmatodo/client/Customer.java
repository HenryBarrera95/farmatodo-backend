package com.farmatodo.client;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_customer_phone", columnNames = "phone")
        })
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "tx_id")
    private String txId;

    public Customer() {}

    public Customer(String name, String email, String phone, String address, Instant createdAt, String txId) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt;
        this.txId = txId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTxId() { return txId; }
}
