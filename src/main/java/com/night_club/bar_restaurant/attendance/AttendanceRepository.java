package com.night_club.bar_restaurant.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    // Used to reject a duplicate clock-in and to find the open record to
    // close on clock-out - an employee can only have one open shift at a time.
    Optional<AttendanceRecord> findByEmployeeIdAndClockOutTimeIsNull(Long employeeId);

    List<AttendanceRecord> findByEmployeeIdOrderByClockInTimeDesc(Long employeeId);

    List<AttendanceRecord> findAllByOrderByClockInTimeDesc();

    // Manager's "who's on the floor right now" view
    List<AttendanceRecord> findByClockOutTimeIsNull();

    // Manager's date-range search (a single day, a month, or a year - the
    // granularity is just how wide a range the caller passes in).
    List<AttendanceRecord> findByClockInTimeBetweenOrderByClockInTimeDesc(
            LocalDateTime start, LocalDateTime end
    );
}