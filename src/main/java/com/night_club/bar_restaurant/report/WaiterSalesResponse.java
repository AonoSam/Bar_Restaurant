package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;

public record WaiterSalesResponse(
        Long waiterId,
        String waiterName,
        long orderCount,
        BigDecimal totalSales
) {
}