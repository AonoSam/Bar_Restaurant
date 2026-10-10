package com.night_club.bar_restaurant.stock;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        StockMovementType type,
        Integer quantityChange,
        Integer resultingStock,
        Long performedByEmployeeId,
        Long supplierId,
        Long relatedOrderId,
        String reason,
        LocalDateTime createdAt
) {
    public static StockMovementResponse fromEntity(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProductId(),
                movement.getProductName(),
                movement.getType(),
                movement.getQuantityChange(),
                movement.getResultingStock(),
                movement.getPerformedByEmployeeId(),
                movement.getSupplierId(),
                movement.getRelatedOrderId(),
                movement.getReason(),
                movement.getCreatedAt()
        );
    }
}