package com.night_club.bar_restaurant.shifthandover;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftHandoverResponse(
        Long id,
        Long attendanceRecordId,
        Long employeeId,
        String employeeName,
        LocalDateTime shiftStartTime,
        LocalDateTime shiftEndTime,
        BigDecimal cashAmount,
        BigDecimal expectedCashAmount,
        // cashAmount - expectedCashAmount: negative = drawer short, positive = over.
        BigDecimal cashDiscrepancy,
        BigDecimal mpesaAmount,
        BigDecimal totalAmount,
        Integer ordersCount,
        Integer pendingOrdersCount,
        String notes,
        Long recordedByEmployeeId,
        LocalDateTime createdAt
) {
    public static ShiftHandoverResponse fromEntity(ShiftHandover h, String employeeName) {
        return new ShiftHandoverResponse(
                h.getId(),
                h.getAttendanceRecordId(),
                h.getEmployeeId(),
                employeeName,
                h.getShiftStartTime(),
                h.getShiftEndTime(),
                h.getCashAmount(),
                h.getExpectedCashAmount(),
                h.getCashAmount().subtract(h.getExpectedCashAmount()),
                h.getMpesaAmount(),
                h.getCashAmount().add(h.getMpesaAmount()),
                h.getOrdersCount(),
                h.getPendingOrdersCount(),
                h.getNotes(),
                h.getRecordedByEmployeeId(),
                h.getCreatedAt()
        );
    }
}