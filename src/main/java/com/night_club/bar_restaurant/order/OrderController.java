package com.night_club.bar_restaurant.order;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Any authenticated employee can start a draft order (waiter in practice,
    // but not hard-restricted - a manager covering the floor can too)
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        Long waiterId = principal.getEmployee().getId();
        return ResponseEntity.ok(orderService.createOrder(request, waiterId));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Long id,
            @Valid @RequestBody AddItemRequest request
    ) {
        return ResponseEntity.ok(orderService.addItem(id, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderResponse> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(orderService.removeItem(id, itemId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    // Defaults to today only when startDate/endDate are omitted - see
    // OrderService.getByWaiter(). Pass one or both (ISO date, YYYY-MM-DD) to
    // search a specific past day, or a wider range for a month/year, same
    // pattern as GET /api/attendance's search.
    @GetMapping("/mine")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long waiterId = principal.getEmployee().getId();
        return ResponseEntity.ok(orderService.getByWaiter(waiterId, startDate, endDate));
    }

    // Hides one order from MY history only - OrderService.hideOrder() checks
    // the order actually belongs to the requester, so this can't be used to
    // clear someone else's order off their list.
    @PatchMapping("/{id}/hide")
    public ResponseEntity<Void> hideOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        orderService.hideOrder(id, principal.getEmployee().getId());
        return ResponseEntity.noContent().build();
    }

    // Bulk equivalent - clears every order I've ever placed from my own
    // history view, all at once.
    @PostMapping("/mine/clear")
    public ResponseEntity<Void> clearMyHistory(@AuthenticationPrincipal EmployeePrincipal principal) {
        orderService.clearMyHistory(principal.getEmployee().getId());
        return ResponseEntity.noContent().build();
    }

    // Feed the counter dashboard polls (or, once WebSocket lands, this becomes
    // the initial-load endpoint while the socket handles live updates after)
    @GetMapping("/counter")
    @PreAuthorize("hasAnyRole('COUNTER', 'MANAGER')")
    public ResponseEntity<List<OrderResponse>> getActiveCounterOrders() {
        return ResponseEntity.ok(orderService.getActiveCounterOrders());
    }

    // Waiter locks the cart before payment is requested
    @PostMapping("/{id}/submit")
    public ResponseEntity<OrderResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.submit(id));
    }

    // Counter/manager transitions: SUBMITTED->PREPARING->READY->COLLECTED, or ->CANCELLED
    // Route already restricted to COUNTER/MANAGER in SecurityConfig for /api/orders/*/status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COUNTER', 'MANAGER')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.status()));
    }
}