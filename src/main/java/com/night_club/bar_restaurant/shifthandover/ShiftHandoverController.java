package com.night_club.bar_restaurant.shifthandover;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Cash handling -> COUNTER/MANAGER only, same trust boundary
// PaymentController already draws around recordCashPayment. Gated at class
// level with @PreAuthorize (relies on the existing anyRequest().authenticated()
// baseline in SecurityConfig, no new URL matcher needed there).
@RestController
@RequestMapping("/api/shift-handovers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COUNTER', 'MANAGER')")
public class ShiftHandoverController {

    private final ShiftHandoverService shiftHandoverService;

    @PostMapping
    public ResponseEntity<ShiftHandoverResponse> create(
            @Valid @RequestBody CreateShiftHandoverRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftHandoverService.create(request, principal.getEmployee().getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftHandoverResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftHandoverService.getById(id));
    }

    // 204 (not 404) when nothing's been recorded yet - normal first-run state.
    @GetMapping("/latest")
    public ResponseEntity<ShiftHandoverResponse> getLatest() {
        ShiftHandoverResponse latest = shiftHandoverService.getLatest();
        return latest == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(latest);
    }

    @GetMapping
    public ResponseEntity<List<ShiftHandoverResponse>> getAll() {
        return ResponseEntity.ok(shiftHandoverService.getAll());
    }

    @GetMapping("/me")
    public ResponseEntity<List<ShiftHandoverResponse>> getMyHistory(@AuthenticationPrincipal EmployeePrincipal principal) {
        return ResponseEntity.ok(shiftHandoverService.getMyHistory(principal.getEmployee().getId()));
    }
}