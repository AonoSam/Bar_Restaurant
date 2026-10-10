package com.night_club.bar_restaurant.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StkPushRequest(
        @NotNull(message = "Order id is required") Long orderId,

        // Accepts 07XXXXXXXX, 01XXXXXXXX, 2547XXXXXXXX, 2541XXXXXXXX, or +254 variants.
        // Normalized server-side via PhoneUtil before hitting Daraja - the frontend
        // does not need to pre-format this.
        @NotBlank(message = "Phone number is required")
        String phoneNumber
) {
}