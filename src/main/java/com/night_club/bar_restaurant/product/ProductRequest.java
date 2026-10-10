package com.night_club.bar_restaurant.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required") String name,
        @NotNull(message = "Category is required") ProductCategory category,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0") BigDecimal price,
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative") Integer stock,
        String imageUrl,
        // Optional - defaults to 10 in the service if not provided
        @Min(value = 0, message = "Low stock threshold cannot be negative") Integer lowStockThreshold,
        Long supplierId
) {
}