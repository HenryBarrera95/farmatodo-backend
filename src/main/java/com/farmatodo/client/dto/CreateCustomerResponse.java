package com.farmatodo.client.dto;

import java.util.UUID;

public record CreateCustomerResponse(UUID id, String name, String email, String phone, String address) {
}
