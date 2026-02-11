package com.farmatodo.log;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transaction_logs")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "tx_id")
    private String txId;

    @Column(nullable = false, name = "event_type")
    private String eventType;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, length = 1024)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    public TransactionLog() {}

    public TransactionLog(String txId, String eventType, String level, String message,
                          Map<String, Object> payload, Instant createdAt) {
        this.txId = txId;
        this.eventType = eventType;
        this.level = level;
        this.message = message;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getTxId() { return txId; }
    public String getEventType() { return eventType; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
