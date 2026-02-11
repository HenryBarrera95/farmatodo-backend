package com.farmatodo.token.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateTokenRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{13,19}$", message = "cardNumber must be digits 13-19")
    private String cardNumber;

    @NotBlank
    @Size(min = 3, max = 4)
    private String cvv;

    @NotBlank
    private String expiryMonth;

    @NotBlank
    private String expiryYear;

    @NotBlank
    private String cardHolderName;

    public String getCardNumber() { return cardNumber; }
    public String getCvv() { return cvv; }
    public String getExpiryMonth() { return expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public String getCardHolderName() { return cardHolderName; }

    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
}
