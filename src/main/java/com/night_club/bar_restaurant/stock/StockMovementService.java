package com.night_club.bar_restaurant.stock;

import com.night_club.bar_restaurant.product.Product;
import com.night_club.bar_restaurant.product.ProductRepository;
import com.night_club.bar_restaurant.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    @Transactional
    public StockMovementResponse restock(Long productId, RestockRequest request, Long performedByEmployeeId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        int newStock = product.getStock() + request.quantity();
        product.setStock(newStock);

        if (newStock > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .productName(product.getName())
                .type(StockMovementType.RESTOCK)
                .quantityChange(request.quantity())
                .resultingStock(newStock)
                .performedByEmployeeId(performedByEmployeeId)
                .supplierId(request.supplierId())
                .reason(request.notes())
                .build();

        return StockMovementResponse.fromEntity(stockMovementRepository.save(movement));
    }

    @Transactional
    public StockMovementResponse adjustStock(Long productId, AdjustStockRequest request, Long performedByEmployeeId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        int newStock = product.getStock() + request.quantityChange();
        if (newStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Adjustment would make stock negative (current stock: " + product.getStock() + ")");
        }

        product.setStock(newStock);
        if (newStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        productRepository.save(product);

        StockMovementType type = request.type() != null ? request.type() : StockMovementType.ADJUSTMENT;
        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .productName(product.getName())
                .type(type)
                .quantityChange(request.quantityChange())
                .resultingStock(newStock)
                .performedByEmployeeId(performedByEmployeeId)
                .reason(request.reason())
                .build();

        return StockMovementResponse.fromEntity(stockMovementRepository.save(movement));
    }

    /**
     * Called by StockDeductionListener AFTER it has already decremented and
     * saved the product's stock - this method only records the audit row,
     * it does not touch Product itself, since that flow already owns the
     * mutation (including its own oversell-clamping logic).
     */
    @Transactional
    public void recordSaleMovement(Product product, int quantityDeducted, Long orderId) {
        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .productName(product.getName())
                .type(StockMovementType.SALE)
                .quantityChange(-quantityDeducted)
                .resultingStock(product.getStock())
                .relatedOrderId(orderId)
                .build();
        stockMovementRepository.save(movement);
    }

    public List<StockMovementResponse> getHistoryForProduct(Long productId, int limit) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(0, limit))
                .stream()
                .map(StockMovementResponse::fromEntity)
                .toList();
    }

    public List<StockMovementResponse> getRecentHistory(int limit) {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(StockMovementResponse::fromEntity)
                .toList();
    }
}