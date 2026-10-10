package com.night_club.bar_restaurant.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByReservationDateTimeBetweenOrderByReservationDateTimeAsc(
            LocalDateTime start, LocalDateTime end
    );

    List<Reservation> findByStatusOrderByReservationDateTimeAsc(ReservationStatus status);

    // Used by the reminder scheduler - only CONFIRMED reservations get a
    // reminder (a PENDING one with no deposit yet isn't locked in), within
    // the upcoming window, that haven't already been reminded about.
    List<Reservation> findByStatusAndReminderSentFalseAndReservationDateTimeBetween(
            ReservationStatus status, LocalDateTime start, LocalDateTime end
    );
}