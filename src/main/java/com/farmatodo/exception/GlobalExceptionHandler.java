package com.farmatodo.exception;

import com.farmatodo.cart.CartException;
import com.farmatodo.client.CustomerConflictException;
import com.farmatodo.config.TxFilter;
import com.farmatodo.token.TokenRejectedException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex) {
        String tx = MDC.get(TxFilter.TX_ID);
        ApiError err = new ApiError(tx, "Internal Server Error", ex.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String tx = MDC.get(TxFilter.TX_ID);
        ApiError err = new ApiError(tx, "Validation Failed", ex.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(TokenRejectedException.class)
    public ResponseEntity<ApiError> handleRejected(TokenRejectedException ex) {
        String tx = MDC.get(TxFilter.TX_ID);
        ApiError err = new ApiError(tx, "Token Rejected", ex.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err);
    }

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ApiError> handleCart(CartException ex) {
        String tx = MDC.get(TxFilter.TX_ID);
        ApiError err = new ApiError(tx, "Cart Error", ex.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(CustomerConflictException.class)
    public ResponseEntity<ApiError> handleCustomerConflict(CustomerConflictException ex) {
        String tx = MDC.get(TxFilter.TX_ID);
        ApiError err = new ApiError(tx, "Conflict", ex.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String tx = MDC.get(TxFilter.TX_ID);
        String message = inferConflictMessage(ex);
        ApiError err = new ApiError(tx, "Conflict", message, Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    private String inferConflictMessage(DataIntegrityViolationException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
            if (t.getMessage() != null) {
                msg = msg + " " + t.getMessage();
            }
        }
        if (msg.contains("uk_customer_email")) {
            return "Email already registered";
        }
        if (msg.contains("uk_customer_phone")) {
            return "Phone already registered";
        }
        return "Duplicate resource";
    }
}
