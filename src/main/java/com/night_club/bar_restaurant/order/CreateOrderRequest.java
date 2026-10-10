package com.night_club.bar_restaurant.order;

public record CreateOrderRequest(
        String tableNumber,     // nullable - walk-in orders have no table
        String customerPhone    // nullable at creation, required before STK push later
) {
}