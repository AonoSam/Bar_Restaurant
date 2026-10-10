package com.night_club.bar_restaurant.notification;

import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderPaidEvent;
import com.night_club.bar_restaurant.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidNotificationListener {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    // AFTER_COMMIT, same reasoning as every other listener on this event -
    // never notify about a payment that could still roll back.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("OrderPaidNotificationListener: order {} not found, skipping notification", event.orderId());
            return;
        }

        notificationService.create(
                NotificationType.NEW_PAID_ORDER,
                "New paid order",
                order.getOrderNumber() + " - KES " + order.getTotal().toPlainString(),
                order.getId()
        );
    }
}