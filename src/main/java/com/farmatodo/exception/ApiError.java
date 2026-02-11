package com.farmatodo.exception;

import java.time.Instant;

public record ApiError(String txId, String message, String detail, Instant timestamp) {
}
