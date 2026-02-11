package com.farmatodo.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AesEncryptionService")
class AesEncryptionServiceTest {

    private static final String VALID_KEY_B64 = "QmFja2VuZEVuY3J5cHRpb25LZXlGb3JERVZfMjU2X0dDTQ==";

    @Test
    @DisplayName("encrypt produce ciphertext, iv y authTag distintos cada vez")
    void encrypt_producesResult() {
        var service = new AesEncryptionService(VALID_KEY_B64);
        var result1 = service.encrypt("4111111111111111");
        var result2 = service.encrypt("4111111111111111");

        assertThat(result1.ciphertext()).isNotNull().isNotEmpty();
        assertThat(result1.iv()).isNotNull().hasSize(12);
        assertThat(result1.authTag()).isNotNull().hasSize(16);
        assertThat(result1.ciphertext()).isNotEqualTo(result2.ciphertext());
        assertThat(result1.iv()).isNotEqualTo(result2.iv());
    }

    @Test
    @DisplayName("acepta key con espacios extra (trim)")
    void encrypt_acceptsTrimmedKey() {
        var keyWithSpaces = "  " + VALID_KEY_B64 + "  ";
        var service = new AesEncryptionService(keyWithSpaces);
        var result = service.encrypt("test");
        assertThat(result).isNotNull();
    }
}
