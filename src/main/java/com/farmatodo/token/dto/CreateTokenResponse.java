package com.farmatodo.token.dto;

public class CreateTokenResponse {
    private String token;
    private String maskedPan;

    public CreateTokenResponse(String token, String maskedPan) {
        this.token = token;
        this.maskedPan = maskedPan;
    }

    public String getToken() { return token; }
    public String getMaskedPan() { return maskedPan; }
}
