package com.night_club.bar_restaurant.shifttransfer;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// No blanket role restriction on this controller - Waiter and Counter are
// exactly who this feature is for (create/accept/reject/cancel/incoming/sent
// all rely on the baseline anyRequest().authenticated() rule already in
// SecurityConfig). MANAGER is explicitly rejected at the business-rule level
// inside ShiftTransferService.create(), not here - that's a data rule
// ("is this employee's role MANAGER"), not a URL-level one. Only getAll()
// (manager oversight of every request) is role-gated, via @PreAuthorize below.
@RestController
@RequestMapping("/api/shift-transfers")
@RequiredArgsConstructor
public class ShiftTransferController {

    private final ShiftTransferService shiftTransferService;

    @PostMapping
    public ResponseEntity<ShiftTransferRequestResponse> create(
            @Valid @RequestBody CreateShiftTransferRequest request,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftTransferService.create(request, principal.getEmployee().getId()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ShiftTransferRequestResponse> accept(
            @PathVariable Long id,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftTransferService.accept(id, principal.getEmployee().getId()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ShiftTransferRequestResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftTransferService.reject(id, principal.getEmployee().getId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ShiftTransferRequestResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftTransferService.cancel(id, principal.getEmployee().getId()));
    }

    // Pending requests waiting for MY response.
    @GetMapping("/incoming")
    public ResponseEntity<List<ShiftTransferRequestResponse>> getIncoming(
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftTransferService.getIncoming(principal.getEmployee().getId()));
    }

    // Requests I've sent, any status.
    @GetMapping("/sent")
    public ResponseEntity<List<ShiftTransferRequestResponse>> getSent(
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        return ResponseEntity.ok(shiftTransferService.getSent(principal.getEmployee().getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ShiftTransferRequestResponse>> getAll() {
        return ResponseEntity.ok(shiftTransferService.getAll());
    }
}