package com.night_club.bar_restaurant.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByWaiterId(Long waiterId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    Optional<Order> findByOrderNumber(String orderNumber);

    // Used to generate today's sequential order number (ORD-YYYYMMDD-NNNN)
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Counter dashboard: only orders that are paid AND actively in the kitchen/bar pipeline
    List<Order> findByPaymentStatusAndStatusIn(PaymentStatus paymentStatus, List<OrderStatus> statuses);

    // Shift/sales reports: paid orders within a date range
    List<Order> findByPaymentStatusAndCreatedAtBetween(
            PaymentStatus paymentStatus, LocalDateTime start, LocalDateTime end
    );

    // "My Orders" screen: a waiter's own, non-hidden orders within a date
    // range (defaults to today - see OrderService.getByWaiter), or a wider
    // range when the waiter searches a past day/month/year. Hidden orders
    // (cleared from history) are excluded here but nowhere else.
    List<Order> findByWaiterIdAndHiddenFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long waiterId, LocalDateTime start, LocalDateTime end
    );
}