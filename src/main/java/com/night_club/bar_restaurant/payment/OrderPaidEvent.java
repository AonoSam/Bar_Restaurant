package com.night_club.bar_restaurant.order;

/**
 * Published once a Payment callback confirms an order as PAID.
 * The stock-deduction module (upcoming) should listen to this via
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) -
 * AFTER_COMMIT specifically, so a rollback on the payment transaction
 * can never leave stock decremented with no matching paid order.
 */
public record OrderPaidEvent(Long orderId) {
}