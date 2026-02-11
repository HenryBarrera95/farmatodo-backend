package com.farmatodo.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM implementation of EncryptionService.
 * IV is 12 bytes, auth tag 128 bits. One IV per encryption.
 */
@Service
public class AesEncryptionService implements EncryptionService {

    private static final String ALGO = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int AUTH_TAG_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public AesEncryptionService(@Value("${ENCRYPTION_KEY:}") String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException("ENCRYPTION_KEY must be provided for encryption");
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        this.key = new SecretKeySpec(keyBytes, ALGO);
    }

    @Override
    public EncryptionResult encrypt(String plainText) {
        try {
            byte[] iv = randomIv();
            byte[] plainBytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] cipherWithTag = cipher.doFinal(plainBytes);

            int ctLen = cipherWithTag.length - AUTH_TAG_BYTES;
            byte[] ciphertext = new byte[ctLen];
            byte[] authTag = new byte[AUTH_TAG_BYTES];
            System.arraycopy(cipherWithTag, 0, ciphertext, 0, ctLen);
            System.arraycopy(cipherWithTag, ctLen, authTag, 0, AUTH_TAG_BYTES);

            return new EncryptionResult(ciphertext, iv, authTag);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private static byte[] randomIv() {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        return iv;
    }
}
