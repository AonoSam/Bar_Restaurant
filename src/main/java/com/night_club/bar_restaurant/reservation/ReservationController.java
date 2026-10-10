package com.night_club.bar_restaurant.reservation;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Every endpoint here is COUNTER/MANAGER only - enforced in SecurityConfig
// via .requestMatchers("/api/reservations/**").hasAnyRole("COUNTER", "MANAGER").
// Waiters have no access to this controller at all, per the requirement that
// only counter staff and managers can create/manage VIP reservations.
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<ReservationResponse>> getUpcoming() {
        return ResponseEntity.ok(reservationService.getUpcoming());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(reservationService.create(request, principal.getEmployee().getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    // "Setting their payments once" - a one-time deposit record. Rejects if
    // a deposit has already been recorded (see ReservationService.recordDeposit).
    @PostMapping("/{id}/deposit")
    public ResponseEntity<ReservationResponse> recordDeposit(
            @PathVariable Long id,
            @Valid @RequestBody RecordDepositRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(reservationService.recordDeposit(id, request, principal.getEmployee().getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationStatusRequest request
    ) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request.status()));
    }
}