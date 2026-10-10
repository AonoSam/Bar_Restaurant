package com.night_club.bar_restaurant.reservation;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationRequest(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotBlank(message = "Customer phone is required") String customerPhone,
        @NotBlank(message = "Table/section is required") String tableNumber,
        @NotNull(message = "Number of guests is required")
        @Min(value = 1, message = "Must be at least 1 guest") Integer numberOfGuests,
        @NotNull(message = "Reservation date/time is required")
        @Future(message = "Reservation must be in the future") LocalDateTime reservationDateTime,
        // Optional - a reservation can exist with no deposit required
        @DecimalMin(value = "0.0", inclusive = false, message = "Deposit amount must be greater than 0")
        BigDecimal depositAmount,
        String notes
) {
}