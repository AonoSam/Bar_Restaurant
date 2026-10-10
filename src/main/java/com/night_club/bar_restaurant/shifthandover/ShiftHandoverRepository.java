package com.night_club.bar_restaurant.shifthandover;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {

    Optional<ShiftHandover> findByAttendanceRecordId(Long attendanceRecordId);

    List<ShiftHandover> findAllByOrderByShiftEndTimeDesc();

    // What the next shift sees first - the single most recent handover, so
    // "the next shift starts with a clear summary" as specified.
    Optional<ShiftHandover> findFirstByOrderByShiftEndTimeDesc();

    List<ShiftHandover> findByEmployeeIdOrderByShiftEndTimeDesc(Long employeeId);
}