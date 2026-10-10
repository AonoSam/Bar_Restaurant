package com.night_club.bar_restaurant.reservation;

import com.night_club.bar_restaurant.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecordDepositRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
        BigDecimal amount,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
        // M-Pesa receipt code if paid via M-Pesa, or a free-text note if cash.
        // Not verified against Daraja - this is a manually-recorded payment.
        String reference
) {
}