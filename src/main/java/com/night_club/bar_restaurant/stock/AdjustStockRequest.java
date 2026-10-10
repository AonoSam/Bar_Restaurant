package com.night_club.bar_restaurant.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
        // Signed - positive to add (e.g. found extra stock during a count),
        // negative to remove (e.g. breakage, spoilage, count correction down)
        @NotNull(message = "Quantity change is required") Integer quantityChange,
        @NotBlank(message = "A reason is required for manual stock adjustments") String reason,
        // WASTE vs ADJUSTMENT - defaults to ADJUSTMENT if not specified
        StockMovementType type
) {
}