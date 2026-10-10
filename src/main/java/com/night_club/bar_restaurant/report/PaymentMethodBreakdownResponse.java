package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;

public record PaymentMethodBreakdownResponse(
        BigDecimal cashTotal,
        BigDecimal mpesaTotal,
        double cashPercentage,
        double mpesaPercentage
) {
}