package com.night_club.bar_restaurant.reservation;

import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusRequest(
        @NotNull(message = "Status is required") ReservationStatus status
) {
}