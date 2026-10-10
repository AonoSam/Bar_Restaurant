package com.night_club.bar_restaurant.stock;

import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderPaidEvent;
import com.night_club.bar_restaurant.order.OrderItem;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.notification.NotificationService;
import com.night_club.bar_restaurant.notification.NotificationType;
import com.night_club.bar_restaurant.product.Product;
import com.night_club.bar_restaurant.product.ProductRepository;
import com.night_club.bar_restaurant.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDeductionListener {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final StockMovementService stockMovementService;

    private static final String LOW_STOCK_TOPIC = "/topic/low-stock-alerts";

    // AFTER_COMMIT: the payment transaction has fully committed by the time this
    // runs, so a rollback on the payment side can never leave stock decremented
    // with no matching paid order in the database.
    //
    // REQUIRES_NEW: this listener needs its own fresh transaction, since the
    // original transaction (the one that published the event) has already
    // committed and closed by the time AFTER_COMMIT fires.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPaid(OrderPaidEvent event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null) {
            log.warn("StockDeductionListener: order {} not found, skipping stock deduction", event.orderId());
            return;
        }

        for (OrderItem item : order.getItems()) {
            deductStock(item.getProductId(), item.getQuantity(), order.getId());
        }
    }

    private void deductStock(Long productId, int quantity, Long orderId) {
        // Pessimistic write lock: holds this row until the transaction commits,
        // so a second concurrent payment for the same product waits its turn
        // instead of both reading the same stale stock value.
        Product product = productRepository.findByIdForUpdate(productId).orElse(null);

        if (product == null) {
            log.warn("StockDeductionListener: product {} not found, skipping deduction", productId);
            return;
        }

        int newStock = product.getStock() - quantity;

        if (newStock < 0) {
            // Can happen if stock was already low/zero by the time payment cleared
            // (e.g. two orders for the last 2 units both got submitted before either
            // was paid). Clamp at 0 rather than storing a negative number, but log
            // it loudly since it means oversold inventory that needs a manual check.
            log.warn("Product {} oversold: stock would go to {} (had {}, deducting {}). Clamping to 0.",
                    product.getName(), newStock, product.getStock(), quantity);
            newStock = 0;
        }

        product.setStock(newStock);

        if (newStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        productRepository.save(product);

        // Audit trail row - this is what makes the deduction that just
        // happened actually queryable later, instead of only ever existing
        // as a log line and a silently-updated Product.stock number.
        stockMovementService.recordSaleMovement(product, quantity, orderId);

        // Alert on crossing zero, or on landing at/under the threshold while still > 0.
        // No "just crossed" check against the pre-deduction value - if a product
        // sits below threshold across multiple sales, the alert re-fires each time,
        // which is the safer default for something a manager needs to act on.
        if (newStock <= product.getLowStockThreshold()) {
            broadcastLowStockAlert(product);
        }
    }

    private void broadcastLowStockAlert(Product product) {
        LowStockAlertResponse alert = new LowStockAlertResponse(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getLowStockThreshold()
        );
        messagingTemplate.convertAndSend(LOW_STOCK_TOPIC, alert);
        log.info("Low stock alert: {} has {} left (threshold {})",
                product.getName(), product.getStock(), product.getLowStockThreshold());

        // Also persisted as a Notification, separate from the raw
        // /topic/low-stock-alerts push above - that topic only reaches
        // whoever's connected right now, this is what shows up later in the
        // notification history even for a manager who wasn't online at the time.
        String title = product.getStock() == 0 ? "Out of stock" : "Low stock";
        notificationService.create(
                NotificationType.LOW_STOCK,
                title,
                product.getName() + " - " + product.getStock() + " left (threshold "
                        + product.getLowStockThreshold() + ")",
                product.getId()
        );
    }
}