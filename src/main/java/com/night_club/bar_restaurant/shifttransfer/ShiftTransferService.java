package com.night_club.bar_restaurant.shifttransfer;

import com.night_club.bar_restaurant.attendance.AttendanceRepository;
import com.night_club.bar_restaurant.employee.Employee;
import com.night_club.bar_restaurant.employee.EmployeeRepository;
import com.night_club.bar_restaurant.employee.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftTransferService {

    private final ShiftTransferRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    public ShiftTransferRequestResponse create(CreateShiftTransferRequest request, Long requesterId) {
        Employee requester = findEmployeeOrThrow(requesterId);
        Employee recipient = findEmployeeOrThrow(request.recipientId());

        if (requester.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot hand your shift over to yourself");
        }

        // Scope: only WAITER and COUNTER use this peer-to-peer flow -
        // MANAGER doesn't need another manager to accept before ending a shift.
        if (requester.getRole() == Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Shift transfer requests are only for waiters and counters");
        }

        if (requester.getRole() != recipient.getRole()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can only hand your shift over to someone with the same role");
        }

        // You can only hand over a shift you're currently working.
        attendanceRepository.findByEmployeeIdAndClockOutTimeIsNull(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "You must be clocked in to request a shift transfer"));

        requestRepository.findFirstByRequesterIdAndStatus(requesterId, ShiftTransferStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "You already have a pending shift transfer request");
                });

        ShiftTransferRequest saved = requestRepository.save(ShiftTransferRequest.builder()
                .requesterId(requesterId)
                .recipientId(recipient.getId())
                .role(requester.getRole())
                .reason(request.reason())
                .note(request.note())
                .status(ShiftTransferStatus.PENDING)
                .build());

        return toResponse(saved);
    }

    @Transactional
    public ShiftTransferRequestResponse accept(Long requestId, Long recipientId) {
        ShiftTransferRequest transferRequest = findPendingOwnedByRecipient(requestId, recipientId);

        // Confirmed rule: the recipient must already be clocked in themselves -
        // accepting only transfers responsibility, it never clocks anyone
        // in or out on their behalf.
        attendanceRepository.findByEmployeeIdAndClockOutTimeIsNull(recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "You must be clocked in to accept a shift transfer"));

        transferRequest.setStatus(ShiftTransferStatus.ACCEPTED);
        transferRequest.setRespondedAt(LocalDateTime.now());
        return toResponse(requestRepository.save(transferRequest));
    }

    @Transactional
    public ShiftTransferRequestResponse reject(Long requestId, Long recipientId) {
        ShiftTransferRequest transferRequest = findPendingOwnedByRecipient(requestId, recipientId);

        transferRequest.setStatus(ShiftTransferStatus.REJECTED);
        transferRequest.setRespondedAt(LocalDateTime.now());
        return toResponse(requestRepository.save(transferRequest));
    }

    @Transactional
    public ShiftTransferRequestResponse cancel(Long requestId, Long requesterId) {
        ShiftTransferRequest transferRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shift transfer request not found: " + requestId));

        if (!transferRequest.getRequesterId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the person who sent this request can cancel it");
        }
        if (transferRequest.getStatus() != ShiftTransferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a pending request can be cancelled - this one is " + transferRequest.getStatus());
        }

        transferRequest.setStatus(ShiftTransferStatus.CANCELLED);
        transferRequest.setRespondedAt(LocalDateTime.now());
        return toResponse(requestRepository.save(transferRequest));
    }

    public List<ShiftTransferRequestResponse> getIncoming(Long recipientId) {
        return requestRepository
                .findByRecipientIdAndStatusOrderByCreatedAtDesc(recipientId, ShiftTransferStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ShiftTransferRequestResponse> getSent(Long requesterId) {
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Manager-only oversight.
    public List<ShiftTransferRequestResponse> getAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private ShiftTransferRequest findPendingOwnedByRecipient(Long requestId, Long recipientId) {
        ShiftTransferRequest transferRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shift transfer request not found: " + requestId));

        if (!transferRequest.getRecipientId().equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This request wasn't sent to you");
        }
        if (transferRequest.getStatus() != ShiftTransferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This request has already been " + transferRequest.getStatus().name().toLowerCase());
        }
        return transferRequest;
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + id));
    }

    private ShiftTransferRequestResponse toResponse(ShiftTransferRequest r) {
        String requesterName = employeeRepository.findById(r.getRequesterId()).map(Employee::getName).orElse("Unknown");
        String recipientName = employeeRepository.findById(r.getRecipientId()).map(Employee::getName).orElse("Unknown");
        return ShiftTransferRequestResponse.fromEntity(r, requesterName, recipientName);
    }
}