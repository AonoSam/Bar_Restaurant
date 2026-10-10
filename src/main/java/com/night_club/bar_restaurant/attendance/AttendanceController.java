package com.night_club.bar_restaurant.attendance;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Clock in/out is open to every authenticated role (Waiter/Counter/Manager) -
// everyone tracks their own hours, so those two endpoints rely on the
// baseline anyRequest().authenticated() rule already in SecurityConfig, no
// new matcher needed there. Viewing OTHER people's attendance is manager-only,
// gated per-method with @PreAuthorize - same pattern OrderController and
// PaymentController already use for their role-restricted actions.
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(@AuthenticationPrincipal EmployeePrincipal principal) {
        return ResponseEntity.ok(attendanceService.clockIn(principal.getEmployee().getId()));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(@AuthenticationPrincipal EmployeePrincipal principal) {
        return ResponseEntity.ok(attendanceService.clockOut(principal.getEmployee().getId()));
    }

    // 204 (not 404) when there's no open shift - "not currently clocked in"
    // is a normal state for this particular check, not an error.
    @GetMapping("/me/active")
    public ResponseEntity<AttendanceResponse> getMyActiveShift(@AuthenticationPrincipal EmployeePrincipal principal) {
        AttendanceResponse active = attendanceService.getMyActiveShift(principal.getEmployee().getId());
        return active == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(active);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AttendanceResponse>> getMyHistory(@AuthenticationPrincipal EmployeePrincipal principal) {
        return ResponseEntity.ok(attendanceService.getMyHistory(principal.getEmployee().getId()));
    }

    // startDate/endDate are both optional, ISO date (YYYY-MM-DD) query params
    // - e.g. GET /api/attendance?startDate=2026-07-15&endDate=2026-07-15 for a
    // single day, or startDate=2026-07-01&endDate=2026-07-31 for a month.
    // Omitting both returns everything, same as before this search existed.
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AttendanceResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(attendanceService.getAll(startDate, endDate));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AttendanceResponse>> getActive() {
        return ResponseEntity.ok(attendanceService.getActive());
    }
}