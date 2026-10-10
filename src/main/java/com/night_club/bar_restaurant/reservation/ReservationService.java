package com.night_club.bar_restaurant.reservation;

import com.night_club.bar_restaurant.common.PhoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public List<ReservationResponse> getAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }

    public List<ReservationResponse> getUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        return reservationRepository
                .findByReservationDateTimeBetweenOrderByReservationDateTimeAsc(now, now.plusYears(1))
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }

    public ReservationResponse getById(Long id) {
        return ReservationResponse.fromEntity(findOrThrow(id));
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, Long createdByEmployeeId) {
        String normalizedPhone;
        try {
            normalizedPhone = PhoneUtil.normalize(request.customerPhone());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Reservation reservation = Reservation.builder()
                .customerName(request.customerName())
                .customerPhone(normalizedPhone)
                .tableNumber(request.tableNumber())
                .numberOfGuests(request.numberOfGuests())
                .reservationDateTime(request.reservationDateTime())
                .depositAmount(request.depositAmount())
                .notes(request.notes())
                .createdByEmployeeId(createdByEmployeeId)
                .status(ReservationStatus.PENDING)
                .build();

        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request) {
        Reservation reservation = findOrThrow(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot edit a reservation that is already " + reservation.getStatus());
        }

        String normalizedPhone;
        try {
            normalizedPhone = PhoneUtil.normalize(request.customerPhone());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        reservation.setCustomerName(request.customerName());
        reservation.setCustomerPhone(normalizedPhone);
        reservation.setTableNumber(request.tableNumber());
        reservation.setNumberOfGuests(request.numberOfGuests());
        reservation.setReservationDateTime(request.reservationDateTime());
        reservation.setDepositAmount(request.depositAmount());
        reservation.setNotes(request.notes());

        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse recordDeposit(Long id, RecordDepositRequest request, Long recordedByEmployeeId) {
        Reservation reservation = findOrThrow(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot record a deposit on a cancelled reservation");
        }
        if (reservation.isDepositPaid()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit already recorded for this reservation");
        }

        reservation.setDepositAmount(request.amount());
        reservation.setDepositPaymentMethod(request.paymentMethod());
        reservation.setDepositPaid(true);
        reservation.setDepositReference(request.reference());
        reservation.setDepositRecordedByEmployeeId(recordedByEmployeeId);
        reservation.setDepositRecordedAt(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.CONFIRMED);

        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatus newStatus) {
        Reservation reservation = findOrThrow(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reservation is already " + reservation.getStatus() + " - no further status changes allowed");
        }

        reservation.setStatus(newStatus);
        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found: " + id));
    }
}