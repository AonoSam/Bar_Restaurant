package com.night_club.bar_restaurant.payment;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final DarajaService darajaService;
    private final CashPaymentService cashPaymentService;
    private final OrderRepository orderRepository;

    @PostMapping("/stk-push")
    public ResponseEntity<StkPushResponse> initiateStkPush(@Valid @RequestBody StkPushRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + request.orderId()));

        return ResponseEntity.ok(darajaService.initiateStkPush(order, request.phoneNumber()));
    }

    // Counter/manager confirms cash was physically received before this is called -
    // there's no external verification step like there is with M-Pesa's callback,
    // so access is restricted to roles trusted to handle cash.
    @PostMapping("/cash")
    @PreAuthorize("hasAnyRole('COUNTER', 'MANAGER')")
    public ResponseEntity<Void> recordCashPayment(
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        cashPaymentService.recordCashPayment(request.orderId(), principal.getEmployee().getId());
        return ResponseEntity.ok().build();
    }

    // Public endpoint - Daraja calls this directly, no JWT available.
    // Already permitAll() in SecurityConfig: .requestMatchers("/api/payments/callback")
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(@RequestBody DarajaCallbackPayload payload) {
        darajaService.handleCallback(payload);
        // Daraja expects a 200 with this exact shape regardless of outcome -
        // returning anything else just triggers a retry storm.
        return ResponseEntity.ok(Map.of("ResultCode", "0", "ResultDesc", "Accepted"));
    }
}