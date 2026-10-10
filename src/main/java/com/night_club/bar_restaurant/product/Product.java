package com.night_club.bar_restaurant.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    // Money as BigDecimal, never float/double - avoids rounding errors on totals
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    // Alert fires when stock drops to or below this value (but is still > 0).
    // Defaulted per-product rather than globally, since a bar sells through
    // spirits and mixers at very different rates.
    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "image_url")
    private String imageUrl;

    // Nullable - not every product needs a tracked supplier. Plain Long, not
    // a JPA @ManyToOne, matching the pattern used for Order.waiterId - keeps
    // this entity free of a lazy-loading relationship it rarely needs to
    // traverse; the frontend resolves supplier name via a separate lookup.
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // A product entered with 0 stock should show as out of stock from the start,
        // not sit as ACTIVE until the next manual edit touches it.
        if (this.stock != null && this.stock <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}