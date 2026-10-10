package com.night_club.bar_restaurant.payment;

import jakarta.validation.constraints.NotNull;

public record CashPaymentRequest(
        @NotNull(message = "Order id is required") Long orderId
        // No amount field on purpose - the amount charged is always the order's
        // own total, never a client-supplied number. Trusting a client-sent
        // amount here would let a cash payment be recorded for less than owed.
) {
}