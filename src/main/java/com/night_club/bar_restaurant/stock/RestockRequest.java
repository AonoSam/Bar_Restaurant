package com.night_club.bar_restaurant.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestockRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Restock quantity must be at least 1") Integer quantity,
        Long supplierId, // optional - null if not tracking which supplier this came from
        String notes
) {
}