package com.farmatodo.token;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "card_tokens")
public class CardToken {

    @Id
    private String token;

    @Column(nullable = false, length = 8192)
    private String ciphertext;

    @Column(nullable = false)
    private String iv;

    @Column(nullable = false)
    private String authTag;

    @Column(nullable = false)
    private String maskedPan;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = true)
    private String txId;

    public CardToken() {}

    public CardToken(String token, String ciphertext, String iv, String authTag, String maskedPan, Instant createdAt, String txId) {
        this.token = token;
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.authTag = authTag;
        this.maskedPan = maskedPan;
        this.createdAt = createdAt;
        this.txId = txId;
    }

    public String getToken() { return token; }
    public String getCiphertext() { return ciphertext; }
    public String getIv() { return iv; }
    public String getAuthTag() { return authTag; }
    public String getMaskedPan() { return maskedPan; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTxId() { return txId; }
}
