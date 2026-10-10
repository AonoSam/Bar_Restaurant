package com.night_club.bar_restaurant.attendance;

import java.time.Duration;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDateTime clockInTime,
        LocalDateTime clockOutTime,
        Long shiftDurationMinutes,   // null while still clocked in
        Long overtimeMinutes,        // null while still clocked in, otherwise max(0, duration - standard)
        boolean isActive
) {
    public static AttendanceResponse fromEntity(
            AttendanceRecord record, String employeeName, long standardShiftMinutes
    ) {
        Long durationMinutes = null;
        Long overtimeMinutes = null;

        if (record.getClockOutTime() != null) {
            durationMinutes = Duration.between(record.getClockInTime(), record.getClockOutTime()).toMinutes();
            overtimeMinutes = Math.max(0, durationMinutes - standardShiftMinutes);
        }

        return new AttendanceResponse(
                record.getId(),
                record.getEmployeeId(),
                employeeName,
                record.getClockInTime(),
                record.getClockOutTime(),
                durationMinutes,
                overtimeMinutes,
                record.isOpen()
        );
    }
}