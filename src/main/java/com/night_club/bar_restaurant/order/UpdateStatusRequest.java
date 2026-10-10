package com.night_club.bar_restaurant.order;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required") OrderStatus status
) {
}