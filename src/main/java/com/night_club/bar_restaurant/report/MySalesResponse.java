package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MySalesResponse(
        LocalDate date,
        long orderCount,
        BigDecimal totalSales,
        BigDecimal averageOrderValue
) {
}