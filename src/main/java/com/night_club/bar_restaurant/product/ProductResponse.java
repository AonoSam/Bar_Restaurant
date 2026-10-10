package com.night_club.bar_restaurant.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        ProductCategory category,
        BigDecimal price,
        Integer stock,
        Integer lowStockThreshold,
        ProductStatus status,
        String imageUrl,
        Long supplierId
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getLowStockThreshold(),
                product.getStatus(),
                product.getImageUrl(),
                product.getSupplierId()
        );
    }
}