package com.night_club.bar_restaurant.payment;

import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderPaidEvent;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.order.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CashPaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Records a cash payment and immediately marks the order PAID.
     * No external call is needed - the counter/waiter physically has the cash
     * in hand at the moment this is called, so there's no PENDING state to wait on.
     */
    @Transactional
    public void recordCashPayment(Long orderId, Long recordedByEmployeeId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is already paid");
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CASH)
                .amount(order.getTotal())
                .status(PaymentStatus.PAID)
                .recordedByEmployeeId(recordedByEmployeeId)
                .build();
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPaidEvent(order.getId()));
    }
}