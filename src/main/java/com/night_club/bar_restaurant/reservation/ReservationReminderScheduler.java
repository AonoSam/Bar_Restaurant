package com.night_club.bar_restaurant.reservation;

import com.night_club.bar_restaurant.notification.NotificationService;
import com.night_club.bar_restaurant.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fires a RESERVATION_REMINDER notification for every CONFIRMED reservation
 * starting within the next 2 hours, once each (guarded by reminderSent).
 *
 * Only CONFIRMED reservations get a reminder - a PENDING one has no deposit
 * recorded yet, so reminding staff about it as if it's locked in would be
 * misleading. If a reservation needs a deposit and none is ever recorded,
 * it simply never reminds - that's the intended behavior, not a bug.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationReminderScheduler {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    private static final int REMINDER_WINDOW_HOURS = 2;

    // Every 5 minutes - frequent enough that no reservation waits long past
    // entering the 2-hour window, without hammering the database constantly.
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void sendUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(REMINDER_WINDOW_HOURS);

        List<Reservation> dueForReminder = reservationRepository
                .findByStatusAndReminderSentFalseAndReservationDateTimeBetween(
                        ReservationStatus.CONFIRMED, now, windowEnd
                );

        for (Reservation reservation : dueForReminder) {
            notificationService.create(
                    NotificationType.RESERVATION_REMINDER,
                    "Upcoming VIP reservation",
                    reservation.getCustomerName() + " - " + reservation.getTableNumber()
                            + " at " + reservation.getReservationDateTime().toLocalTime(),
                    reservation.getId()
            );
            reservation.setReminderSent(true);
            reservationRepository.save(reservation);
        }

        if (!dueForReminder.isEmpty()) {
            log.info("Sent {} reservation reminder(s)", dueForReminder.size());
        }
    }
}