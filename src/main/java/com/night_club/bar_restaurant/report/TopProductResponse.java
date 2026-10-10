package com.night_club.bar_restaurant.report;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productName,
        long quantitySold,
        BigDecimal revenue
) {
}