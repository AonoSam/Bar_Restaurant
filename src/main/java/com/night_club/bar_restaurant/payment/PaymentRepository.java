package com.night_club.bar_restaurant.payment;

import com.night_club.bar_restaurant.order.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);
    Optional<Payment> findByOrderId(Long orderId);

    // Shift reports: cash vs M-Pesa breakdown for a date range
    List<Payment> findByStatusAndCreatedAtBetween(
            PaymentStatus status, LocalDateTime start, LocalDateTime end
    );
}