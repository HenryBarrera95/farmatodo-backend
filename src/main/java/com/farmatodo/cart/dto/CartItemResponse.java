package com.farmatodo.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
