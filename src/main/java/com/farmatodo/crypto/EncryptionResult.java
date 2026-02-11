package com.farmatodo.crypto;

public record EncryptionResult(
        byte[] ciphertext,
        byte[] iv,
        byte[] authTag
) {}
