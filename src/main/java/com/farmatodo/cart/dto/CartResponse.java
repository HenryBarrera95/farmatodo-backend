package com.farmatodo.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(UUID cartId, UUID customerId, List<CartItemResponse> items, BigDecimal total) {
}
