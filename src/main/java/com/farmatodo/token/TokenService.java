package com.farmatodo.token;

import com.farmatodo.config.TxFilter;
import com.farmatodo.crypto.EncryptionResult;
import com.farmatodo.crypto.EncryptionService;
import com.farmatodo.log.LogService;
import com.farmatodo.token.dto.CreateTokenRequest;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final CardTokenRepository repo;
    private final LogService logService;
    private final EncryptionService encryptionService;
    private final double rejectProbability;
    private final Random random = new Random();

    public TokenService(CardTokenRepository repo,
                        LogService logService,
                        EncryptionService encryptionService,
                        @Value("${app.token.reject-probability:0.0}") double rejectProbability) {
        this.repo = repo;
        this.logService = logService;
        this.encryptionService = encryptionService;
        this.rejectProbability = rejectProbability;
    }

    @Transactional
    public CardToken createToken(CreateTokenRequest req) {
        String tx = MDC.get(TxFilter.TX_ID);

        double v = random.nextDouble();
        if (v < rejectProbability) {
            log.info("Tokenization rejected by probability (v={} < p={}) [tx={}]", v, rejectProbability, tx);
            logService.log("token_rejected", "WARN",
                    "Tokenization rejected by configured probability",
                    Map.of("reason", "probability", "randomValue", v, "threshold", rejectProbability));
            throw new TokenRejectedException("Tokenization rejected by configured probability");
        }

        try {
            EncryptionResult encrypted = encryptionService.encrypt(req.getCardNumber());

            String token = UUID.randomUUID().toString();
            String masked = maskPan(req.getCardNumber());

            CardToken entity = new CardToken(
                    token,
                    Base64.getEncoder().encodeToString(encrypted.ciphertext()),
                    Base64.getEncoder().encodeToString(encrypted.iv()),
                    Base64.getEncoder().encodeToString(encrypted.authTag()),
                    masked,
                    Instant.now(),
                    tx
            );

            repo.save(entity);

            logService.log("token_created", "INFO", "Token created successfully",
                    Map.of("token", token, "maskedPan", masked));
            log.info("Token created {} [tx={}]", token, tx);
            return entity;
        } catch (TokenRejectedException tre) {
            throw tre;
        } catch (Exception ex) {
            log.error("Encryption error during tokenization [tx={}]", tx, ex);
            throw new RuntimeException("Encryption failure");
        }
    }

    private String maskPan(String pan) {
        int len = pan.length();
        if (len <= 4) return pan;
        String last4 = pan.substring(len - 4);
        return "**** **** **** " + last4;
    }
}
