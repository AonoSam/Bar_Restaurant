package com.night_club.bar_restaurant.attendance;

import com.night_club.bar_restaurant.employee.Employee;
import com.night_club.bar_restaurant.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    // 8-hour standard shift - anything clocked beyond this on a single
    // clock-in/clock-out pair counts as overtime. This is the one place
    // that policy lives; adjust here if the venue's actual policy differs.
    private static final long STANDARD_SHIFT_MINUTES = 8 * 60;

    @Transactional
    public AttendanceResponse clockIn(Long employeeId) {
        attendanceRepository.findByEmployeeIdAndClockOutTimeIsNull(employeeId)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Already clocked in since " + existing.getClockInTime());
                });

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(employeeId)
                .clockInTime(LocalDateTime.now())
                .build();

        return toResponse(attendanceRepository.save(record));
    }

    @Transactional
    public AttendanceResponse clockOut(Long employeeId) {
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndClockOutTimeIsNull(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not currently clocked in"));

        record.setClockOutTime(LocalDateTime.now());
        return toResponse(attendanceRepository.save(record));
    }

    // Null (not an exception) when there's no open shift - "not clocked in"
    // is a normal state here, not an error condition.
    public AttendanceResponse getMyActiveShift(Long employeeId) {
        return attendanceRepository.findByEmployeeIdAndClockOutTimeIsNull(employeeId)
                .map(this::toResponse)
                .orElse(null);
    }

    public List<AttendanceResponse> getMyHistory(Long employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByClockInTimeDesc(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Manager-only views
    //
    // startDate/endDate are inclusive calendar days, both optional:
    //   - neither given -> everything, same as before this search feature existed
    //   - only one given -> treated as a single-day search (that day only)
    //   - both given -> inclusive range, e.g. a month (1st to last day) or a
    //     year (Jan 1 to Dec 31) - the caller decides the granularity by how
    //     wide a range it passes, this method just applies it.
    public List<AttendanceResponse> getAll(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return attendanceRepository.findAllByOrderByClockInTimeDesc().stream()
                    .map(this::toResponse)
                    .toList();
        }

        LocalDate effectiveStart = startDate != null ? startDate : endDate;
        LocalDate effectiveEnd = endDate != null ? endDate : startDate;

        LocalDateTime start = effectiveStart.atStartOfDay();
        LocalDateTime end = effectiveEnd.plusDays(1).atStartOfDay(); // exclusive upper bound

        return attendanceRepository.findByClockInTimeBetweenOrderByClockInTimeDesc(start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AttendanceResponse> getActive() {
        return attendanceRepository.findByClockOutTimeIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    private AttendanceResponse toResponse(AttendanceRecord record) {
        // Employee entity doesn't get denormalized into AttendanceRecord itself
        // (same as how Order only stores waiterId, not a name snapshot) - name
        // is looked up fresh at read time purely for display convenience.
        String employeeName = employeeRepository.findById(record.getEmployeeId())
                .map(Employee::getName)
                .orElse("Unknown");
        return AttendanceResponse.fromEntity(record, employeeName, STANDARD_SHIFT_MINUTES);
    }
}