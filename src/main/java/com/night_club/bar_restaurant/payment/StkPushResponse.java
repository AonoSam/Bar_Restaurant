package com.night_club.bar_restaurant.payment;

public record StkPushResponse(
        String merchantRequestId,
        String checkoutRequestId,
        String customerMessage
) {
}