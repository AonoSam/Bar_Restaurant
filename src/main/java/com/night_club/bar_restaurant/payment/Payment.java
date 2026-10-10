package com.night_club.bar_restaurant.payment;

import com.night_club.bar_restaurant.order.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.MPESA;

    // Null for cash payments
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "mpesa_receipt")
    private String mpesaReceipt;

    // Null for cash payments - only M-Pesa flows go through Daraja's request/callback cycle
    @Column(name = "merchant_request_id")
    private String merchantRequestId;

    // Nullable + unique: MySQL allows multiple NULLs in a unique column, so
    // any number of cash payments can coexist without checkoutRequestId collisions
    @Column(name = "checkout_request_id", unique = true)
    private String checkoutRequestId;

    // Who recorded a cash payment (waiter/counter/manager employee id).
    // Null for M-Pesa payments, since those are confirmed by Daraja's callback, not a person.
    @Column(name = "recorded_by_employee_id")
    private Long recordedByEmployeeId;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "result_desc")
    private String resultDesc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}