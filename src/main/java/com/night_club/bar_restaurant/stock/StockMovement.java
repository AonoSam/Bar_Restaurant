package com.night_club.bar_restaurant.stock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One row per stock change, regardless of cause. Product.stock only ever
 * holds the current number - this is what makes that number auditable:
 * "why does this product show 4 units" becomes answerable by reading the
 * history instead of just trusting the current count.
 */
@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType type;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "resulting_stock", nullable = false)
    private Integer resultingStock;

    @Column(name = "performed_by_employee_id")
    private Long performedByEmployeeId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}