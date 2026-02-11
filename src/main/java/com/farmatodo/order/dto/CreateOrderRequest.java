package com.farmatodo.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateOrderRequest {

    @NotNull
    private UUID customerId;

    @NotBlank
    @Size(max = 500)
    private String deliveryAddress;

    @NotBlank
    private String token;

    public UUID getCustomerId() { return customerId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getToken() { return token; }

    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public void setToken(String token) { this.token = token; }
}
