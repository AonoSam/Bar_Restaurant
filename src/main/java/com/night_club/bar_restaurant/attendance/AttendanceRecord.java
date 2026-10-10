package com.night_club.bar_restaurant.attendance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "clock_in_time", nullable = false)
    private LocalDateTime clockInTime;

    // Null while the shift is still open - findByEmployeeIdAndClockOutTimeIsNull
    // is how AttendanceService checks "am I already clocked in" and finds the
    // record to close on clock-out.
    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return clockOutTime == null;
    }
}