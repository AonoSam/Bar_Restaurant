package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShiftReportResponse(
        LocalDate date,
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal cashTotal,
        BigDecimal mpesaTotal
) {
}