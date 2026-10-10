package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        // Fixed windows - always computed regardless of the selected range,
        // since these are the "at a glance" numbers a manager checks first.
        BigDecimal todaySales,
        long todayOrders,
        BigDecimal weekSales,   // trailing 7 days including today
        BigDecimal monthSales,  // calendar month to date

        // Everything below is scoped to the requested [rangeStart, rangeEnd]
        LocalDate rangeStart,
        LocalDate rangeEnd,
        List<TopProductResponse> topDrinks,
        List<TopProductResponse> topFood,
        List<HourlySalesResponse> busiestHours, // all 24 hours, zero-filled
        PaymentMethodBreakdownResponse paymentDistribution,
        List<WaiterSalesResponse> topWaiters
) {
}