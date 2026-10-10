package com.night_club.bar_restaurant.report;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;

    // Manager-only (enforced in SecurityConfig via /api/reports/**, MUST stay
    // ordered ahead of the /my-sales carve-out below - see SecurityConfig)
    @GetMapping("/shift")
    public ResponseEntity<ShiftReportResponse> getShiftReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(reportService.getShiftReport(reportDate));
    }

    @GetMapping("/waiters")
    public ResponseEntity<List<WaiterSalesResponse>> getWaiterSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(reportService.getWaiterSales(reportDate));
    }

    // Manager-only CSV export of the full shift report for a given date.
    @GetMapping("/export")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<byte[]> exportShiftCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        String csv = reportService.generateShiftCsv(reportDate);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"shift-report-" + reportDate + ".csv\"")
                .body(bytes);
    }

    // ANY authenticated employee - their own numbers, today only. This must
    // stay listed as an explicit exception in SecurityConfig ahead of the
    // general /api/reports/** -> MANAGER rule, or it inherits that
    // restriction and every waiter gets a 403 here too.
    @GetMapping("/my-sales")
    public ResponseEntity<MySalesResponse> getMySales(@AuthenticationPrincipal EmployeePrincipal principal) {
        Long employeeId = principal.getEmployee().getId();
        return ResponseEntity.ok(reportService.getMySales(employeeId));
    }

    // Manager-only, enforced via SecurityConfig's general /api/reports/**
    // MANAGER rule - no new matcher needed for this one specifically.
    // Range defaults to the current calendar month if not specified.
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate rangeEnd = endDate != null ? endDate : today;
        return ResponseEntity.ok(dashboardService.getDashboard(rangeStart, rangeEnd));
    }
}