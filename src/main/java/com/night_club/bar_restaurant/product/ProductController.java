package com.night_club.bar_restaurant.product;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import com.night_club.bar_restaurant.stock.AdjustStockRequest;
import com.night_club.bar_restaurant.stock.RestockRequest;
import com.night_club.bar_restaurant.stock.StockMovementResponse;
import com.night_club.bar_restaurant.stock.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final StockMovementService stockMovementService;

    // Any authenticated employee (waiter, counter, manager) can browse products
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(
            @RequestParam(required = false) ProductCategory category
    ) {
        List<ProductResponse> products = category != null
                ? productService.getByCategory(category)
                : productService.getAll();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(productService.search(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // Manager-only writes - enforced here AND in SecurityConfig as a second layer
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Manager-only stock actions - each both mutates Product.stock AND writes
    // an auditable StockMovement row in one transaction (see StockMovementService).
    @PostMapping("/{id}/restock")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<StockMovementResponse> restock(
            @PathVariable Long id,
            @Valid @RequestBody RestockRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(stockMovementService.restock(id, request, principal.getEmployee().getId()));
    }

    @PostMapping("/{id}/adjust-stock")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<StockMovementResponse> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody AdjustStockRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(stockMovementService.adjustStock(id, request, principal.getEmployee().getId()));
    }

    @GetMapping("/{id}/stock-movements")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(stockMovementService.getHistoryForProduct(id, limit));
    }
}