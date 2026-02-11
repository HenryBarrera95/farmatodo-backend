package com.farmatodo.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddCartItemRequest {

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID productId;

    @Min(1)
    private int quantity = 1;

    public UUID getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }

    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
