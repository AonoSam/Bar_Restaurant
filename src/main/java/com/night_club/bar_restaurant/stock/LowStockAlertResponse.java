package com.night_club.bar_restaurant.stock;

public record LowStockAlertResponse(
        Long productId,
        String productName,
        int remainingStock,
        int threshold
) {
}