package com.night_club.bar_restaurant.shifthandover;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_handovers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One handover per attendance record - unique constraint backs the
    // explicit check in ShiftHandoverService.create() so a shift can only be
    // closed out once (the service check gives a clean 409 rather than a raw
    // DB constraint violation reaching the client).
    @Column(name = "attendance_record_id", nullable = false, unique = true)
    private Long attendanceRecordId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "shift_start_time", nullable = false)
    private LocalDateTime shiftStartTime;

    @Column(name = "shift_end_time", nullable = false)
    private LocalDateTime shiftEndTime;

    // Physically counted by the outgoing staff member - never computed.
    // That's the whole point of a till count: it catches discrepancies the
    // system alone can't see.
    @Column(name = "cash_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal cashAmount;

    // What the system expects the drawer to hold, based on recorded CASH
    // payments in this window - stored alongside cashAmount purely for
    // reconciliation, never used to override the manual count.
    @Column(name = "expected_cash_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedCashAmount;

    // Auto-summed from PAID MPESA payments in this window - no manual
    // counting needed, Daraja's callback already confirmed each one.
    @Column(name = "mpesa_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal mpesaAmount;

    @Column(name = "orders_count", nullable = false)
    private Integer ordersCount;

    // Orders still active (SUBMITTED/PREPARING/READY) at the moment of
    // handover, not just ones created during this shift - this is what the
    // next shift inherits, so it reflects the current moment, not the window.
    @Column(name = "pending_orders_count", nullable = false)
    private Integer pendingOrdersCount;

    @Column(length = 1000)
    private String notes;

    // Usually the same person whose shift this is, but kept separate from
    // employeeId so a manager can record a handover on someone else's behalf.
    @Column(name = "recorded_by_employee_id", nullable = false)
    private Long recordedByEmployeeId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}