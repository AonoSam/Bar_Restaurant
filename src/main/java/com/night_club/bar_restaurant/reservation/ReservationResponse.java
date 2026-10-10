package com.night_club.bar_restaurant.reservation;

import com.night_club.bar_restaurant.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        String customerName,
        String customerPhone,
        String tableNumber,
        Integer numberOfGuests,
        LocalDateTime reservationDateTime,
        ReservationStatus status,
        BigDecimal depositAmount,
        PaymentMethod depositPaymentMethod,
        boolean depositPaid,
        String depositReference,
        Long depositRecordedByEmployeeId,
        LocalDateTime depositRecordedAt,
        String notes,
        Long createdByEmployeeId,
        LocalDateTime createdAt
) {
    public static ReservationResponse fromEntity(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getTableNumber(),
                r.getNumberOfGuests(),
                r.getReservationDateTime(),
                r.getStatus(),
                r.getDepositAmount(),
                r.getDepositPaymentMethod(),
                r.isDepositPaid(),
                r.getDepositReference(),
                r.getDepositRecordedByEmployeeId(),
                r.getDepositRecordedAt(),
                r.getNotes(),
                r.getCreatedByEmployeeId(),
                r.getCreatedAt()
        );
    }
}