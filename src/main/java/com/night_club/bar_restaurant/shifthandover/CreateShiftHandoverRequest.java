package com.night_club.bar_restaurant.shifthandover;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateShiftHandoverRequest(
        @NotNull(message = "Attendance record id is required") Long attendanceRecordId,
        // Physically counted - required, and can legitimately be 0 but never negative.
        @NotNull(message = "Cash amount is required")
        @DecimalMin(value = "0.0", message = "Cash amount cannot be negative")
        BigDecimal cashAmount,
        String notes
) {
}