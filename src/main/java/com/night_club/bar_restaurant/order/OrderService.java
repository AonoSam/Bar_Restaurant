package com.night_club.bar_restaurant.order;

import com.night_club.bar_restaurant.product.Product;
import com.night_club.bar_restaurant.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Which OrderStatus values are reachable from a given current status.
    // CANCELLED is reachable from DRAFT or SUBMITTED only - once the counter
    // has started preparing a paid order, cancelling needs a separate refund
    // flow, not a plain status flip.
    private static final Set<OrderStatus> FROM_DRAFT = Set.of(OrderStatus.SUBMITTED, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> FROM_SUBMITTED = Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> FROM_PREPARING = Set.of(OrderStatus.READY);
    private static final Set<OrderStatus> FROM_READY = Set.of(OrderStatus.COLLECTED);

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long waiterId) {
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .waiterId(waiterId)
                .tableNumber(request.tableNumber())
                .customerPhone(request.customerPhone())
                .build();

        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse addItem(Long orderId, AddItemRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot modify items on an order that has already been submitted");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + request.productId()));

        // Check if this product is already in the cart - increment quantity instead
        // of adding a duplicate line, so the same drink tapped twice merges into one row.
        var existingItem = order.getItems().stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
            item.recalculateTotal();
        } else {
            OrderItem newItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(request.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            newItem.recalculateTotal();
            order.addItem(newItem);
        }

        order.recalculateTotals();
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse removeItem(Long orderId, Long itemId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot modify items on an order that has already been submitted");
        }

        boolean removed = order.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found: " + itemId);
        }

        order.recalculateTotals();
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    public OrderResponse getById(Long orderId) {
        return OrderResponse.fromEntity(findOrderOrThrow(orderId));
    }

    // "My Orders" screen: defaults to TODAY only (no params), matching the
    // requirement that a waiter sees only the current date's orders by
    // default. startDate/endDate (either or both) switch this into a search
    // over a specific day/month/year - same optional-range pattern already
    // used for attendance search (AttendanceService.getAll()).
    public List<OrderResponse> getByWaiter(Long waiterId, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveStart;
        LocalDate effectiveEnd;

        if (startDate == null && endDate == null) {
            effectiveStart = LocalDate.now();
            effectiveEnd = LocalDate.now();
        } else {
            effectiveStart = startDate != null ? startDate : endDate;
            effectiveEnd = endDate != null ? endDate : startDate;
        }

        LocalDateTime start = effectiveStart.atStartOfDay();
        LocalDateTime end = effectiveEnd.plusDays(1).atStartOfDay(); // exclusive upper bound

        return orderRepository.findByWaiterIdAndHiddenFalseAndCreatedAtBetweenOrderByCreatedAtDesc(waiterId, start, end)
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    // Hides one order from the requesting waiter's own history - never
    // touches paymentStatus, status, or any other field, and never affects
    // what any other role sees. Ownership check stops a waiter clearing
    // someone else's order off their own list.
    @Transactional
    public void hideOrder(Long orderId, Long waiterId) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getWaiterId().equals(waiterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only clear your own orders from history");
        }
        order.setHidden(true);
        orderRepository.save(order);
    }

    // Bulk version of the same thing - hides every order this waiter has
    // ever placed, all-time (not scoped to whatever date range the "My
    // Orders" search happens to be showing right now).
    @Transactional
    public void clearMyHistory(Long waiterId) {
        List<Order> orders = orderRepository.findByWaiterId(waiterId);
        orders.forEach(o -> o.setHidden(true));
        orderRepository.saveAll(orders);
    }

    // Counter dashboard feed: paid orders actively in the pipeline
    public List<OrderResponse> getActiveCounterOrders() {
        return orderRepository.findByPaymentStatusAndStatusIn(
                        PaymentStatus.PAID,
                        List.of(OrderStatus.SUBMITTED, OrderStatus.PREPARING, OrderStatus.READY)
                ).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional
    public OrderResponse submit(Long orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not in DRAFT status");
        }
        if (order.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit an order with no items");
        }

        order.setStatus(OrderStatus.SUBMITTED);
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = findOrderOrThrow(orderId);
        OrderStatus current = order.getStatus();

        if (!isValidTransition(current, newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot move order from " + current + " to " + newStatus);
        }

        // The critical guard: status can never advance past SUBMITTED unless
        // payment has actually cleared. This is what stops a payment glitch
        // (or a counter staff member clicking too fast) from releasing stock
        // or handing over an unpaid order.
        boolean movingIntoKitchenPipeline = newStatus == OrderStatus.PREPARING
                || newStatus == OrderStatus.READY
                || newStatus == OrderStatus.COLLECTED;

        if (movingIntoKitchenPipeline && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order payment is not confirmed (paymentStatus=" + order.getPaymentStatus()
                            + "). Cannot move to " + newStatus + ".");
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(saved.getId()));
        return OrderResponse.fromEntity(saved);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case DRAFT -> FROM_DRAFT.contains(to);
            case SUBMITTED -> FROM_SUBMITTED.contains(to);
            case PREPARING -> FROM_PREPARING.contains(to);
            case READY -> FROM_READY.contains(to);
            case COLLECTED, CANCELLED -> false; // terminal states
        };
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }

    // Format: ORD-YYYYMMDD-NNNN, sequential per day.
    // NOTE: countByCreatedAtBetween + save is not atomic - under heavy concurrent
    // load (two waiters submitting the very first order of the day at the exact
    // same millisecond) this could theoretically produce a duplicate number.
    // Low risk for a single-venue POS, but if that ever matters, replace this
    // with a DB sequence or a UNIQUE constraint + retry-on-conflict.
    private String generateOrderNumber() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        long countToday = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequencePart = String.format("%04d", countToday + 1);

        return "ORD-" + datePart + "-" + sequencePart;
    }
}