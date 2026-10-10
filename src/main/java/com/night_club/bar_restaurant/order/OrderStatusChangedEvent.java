package com.night_club.bar_restaurant.order;

/**
 * Published whenever an order's status transitions (PREPARING, READY, COLLECTED, etc).
 * OrderBroadcastService listens for this and pushes the updated order to
 * /topic/counter-orders so the dashboard updates live without polling.
 */
public record OrderStatusChangedEvent(Long orderId) {
}