package com.farmatodo.order.dto;

import com.farmatodo.order.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID customerId,
        UUID cartId,
        Order.OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        String deliveryAddress,
        String tokenId
) {
}
