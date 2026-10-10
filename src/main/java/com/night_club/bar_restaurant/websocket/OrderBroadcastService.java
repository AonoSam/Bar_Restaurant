package com.night_club.bar_restaurant.websocket;

import com.night_club.bar_restaurant.order.OrderPaidEvent;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.order.OrderResponse;
import com.night_club.bar_restaurant.order.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBroadcastService {

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String COUNTER_TOPIC = "/topic/counter-orders";

    // AFTER_COMMIT, not the default (BEFORE_COMMIT-adjacent) phase: if the
    // payment transaction rolls back for any reason, the counter dashboard
    // must never have been told about an order that doesn't actually exist
    // as PAID in the database.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        broadcast(event.orderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        broadcast(event.orderId());
    }

    private void broadcast(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    OrderResponse response = OrderResponse.fromEntity(order);
                    messagingTemplate.convertAndSend(COUNTER_TOPIC, response);
                    log.debug("Broadcast order {} to {}", order.getOrderNumber(), COUNTER_TOPIC);
                },
                () -> log.warn("Tried to broadcast order {} but it no longer exists", orderId)
        );
    }
}