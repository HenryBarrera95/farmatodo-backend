package com.farmatodo.token.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.YearMonth;

public class CreateTokenRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{13,19}$", message = "cardNumber must be digits 13-19")
    private String cardNumber;

    @NotBlank
    @Size(min = 3, max = 4)
    private String cvv;

    @NotBlank
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "expiryMonth must be 01-12")
    private String expiryMonth;

    @NotBlank
    @Pattern(regexp = "^\\d{2,4}$", message = "expiryYear must be 2 or 4 digits")
    private String expiryYear;

    @NotBlank
    private String cardHolderName;

    @AssertTrue(message = "Card has expired")
    public boolean isExpiryNotInPast() {
        if (expiryMonth == null || expiryYear == null) return true;
        try {
            int month = Integer.parseInt(expiryMonth);
            int year = Integer.parseInt(expiryYear);
            if (year < 100) year += 2000;
            YearMonth expiry = YearMonth.of(year, month);
            return !expiry.isBefore(YearMonth.now());
        } catch (NumberFormatException e) {
            return true; // let @Pattern handle format errors
        }
    }

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
