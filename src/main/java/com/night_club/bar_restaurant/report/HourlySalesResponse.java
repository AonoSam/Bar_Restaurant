package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;

public record HourlySalesResponse(
        int hour,
        long orderCount,
        BigDecimal revenue
) {
}