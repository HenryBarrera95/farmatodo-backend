package com.farmatodo.crypto;

/**
 * Port for encryption operations. Decouples domain logic from the concrete
 * cryptographic algorithm (e.g. AES-GCM). The implementation can be swapped
 * or extended without changing consumers.
 */
public interface EncryptionService {

    /**
     * Encrypts the given plaintext. Generates a random IV per invocation.
     *
     * @param plainText the text to encrypt
     * @return result containing ciphertext, IV, and auth tag
     */
    EncryptionResult encrypt(String plainText);
}
