package com.night_club.bar_restaurant.supplier;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "Supplier name is required") String name,
        String contactPerson,
        String phone,
        String email,
        String address,
        String notes
) {
}