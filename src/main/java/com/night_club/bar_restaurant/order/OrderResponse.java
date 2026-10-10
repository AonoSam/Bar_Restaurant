package com.night_club.bar_restaurant.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long waiterId,
        String tableNumber,
        String customerPhone,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal total,
        PaymentStatus paymentStatus,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getWaiterId(),
                order.getTableNumber(),
                order.getCustomerPhone(),
                order.getItems().stream().map(OrderItemResponse::fromEntity).toList(),
                order.getSubtotal(),
                order.getTotal(),
                order.getPaymentStatus(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}