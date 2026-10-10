package com.night_club.bar_restaurant.reservation;

import com.night_club.bar_restaurant.payment.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    // VIP table/booth/section identifier - free text since venues label these
    // differently ("VIP-1", "Booth 3", "Rooftop Table A")
    @Column(name = "table_number", nullable = false)
    private String tableNumber;

    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "reservation_date_time", nullable = false)
    private LocalDateTime reservationDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    // Deposit is a manually-recorded payment, not a live STK push - see
    // ReservationService.recordDeposit(). Nullable throughout since a
    // reservation can exist with no deposit required at all.
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_payment_method")
    private PaymentMethod depositPaymentMethod;

    @Column(name = "deposit_paid", nullable = false)
    @Builder.Default
    private boolean depositPaid = false;

    // M-Pesa receipt code if paid via M-Pesa, or a free-text note if cash -
    // this is typed in by whoever records the deposit, not verified against Daraja.
    @Column(name = "deposit_reference")
    private String depositReference;

    @Column(name = "deposit_recorded_by_employee_id")
    private Long depositRecordedByEmployeeId;

    @Column(name = "deposit_recorded_at")
    private LocalDateTime depositRecordedAt;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_by_employee_id", nullable = false)
    private Long createdByEmployeeId;

    // Set once the reminder notification has fired for this reservation, so
    // the scheduled job (ReservationReminderScheduler) never sends it twice.
    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}