package com.night_club.bar_restaurant.shifthandover;

import com.night_club.bar_restaurant.attendance.AttendanceRecord;
import com.night_club.bar_restaurant.attendance.AttendanceRepository;
import com.night_club.bar_restaurant.employee.Employee;
import com.night_club.bar_restaurant.employee.EmployeeRepository;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.order.OrderStatus;
import com.night_club.bar_restaurant.order.PaymentStatus;
import com.night_club.bar_restaurant.payment.Payment;
import com.night_club.bar_restaurant.payment.PaymentMethod;
import com.night_club.bar_restaurant.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository shiftHandoverRepository;
    private final AttendanceRepository attendanceRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public ShiftHandoverResponse create(CreateShiftHandoverRequest request, Long recordedByEmployeeId) {
        AttendanceRecord attendance = attendanceRepository.findById(request.attendanceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Attendance record not found: " + request.attendanceRecordId()));

        if (attendance.isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot hand over a shift that hasn't clocked out yet");
        }

        shiftHandoverRepository.findByAttendanceRecordId(attendance.getId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A handover has already been recorded for this shift");
                });

        LocalDateTime shiftStart = attendance.getClockInTime();
        LocalDateTime shiftEnd = attendance.getClockOutTime();

        // PaymentRepository.findByStatusAndCreatedAtBetween was already built
        // with "Shift reports: cash vs M-Pesa breakdown for a date range" in
        // mind - split the single query result by method rather than issuing
        // two separate queries.
        List<Payment> payments = paymentRepository.findByStatusAndCreatedAtBetween(
                PaymentStatus.PAID, shiftStart, shiftEnd
        );

        BigDecimal mpesaAmount = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.MPESA)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedCashAmount = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // OrderRepository.findByPaymentStatusAndCreatedAtBetween: same "shift/
        // sales reports" comment on the repository - paid orders in the window.
        int ordersCount = orderRepository
                .findByPaymentStatusAndCreatedAtBetween(PaymentStatus.PAID, shiftStart, shiftEnd)
                .size();

        // Deliberately NOT windowed by shiftStart/shiftEnd - this reuses the
        // exact same query the counter dashboard uses (getActiveCounterOrders),
        // because "pending" here means "unresolved right now", which the next
        // shift needs regardless of when those orders were originally placed.
        int pendingOrdersCount = orderRepository
                .findByPaymentStatusAndStatusIn(
                        PaymentStatus.PAID,
                        List.of(OrderStatus.SUBMITTED, OrderStatus.PREPARING, OrderStatus.READY)
                )
                .size();

        ShiftHandover handover = ShiftHandover.builder()
                .attendanceRecordId(attendance.getId())
                .employeeId(attendance.getEmployeeId())
                .shiftStartTime(shiftStart)
                .shiftEndTime(shiftEnd)
                .cashAmount(request.cashAmount())
                .expectedCashAmount(expectedCashAmount)
                .mpesaAmount(mpesaAmount)
                .ordersCount(ordersCount)
                .pendingOrdersCount(pendingOrdersCount)
                .notes(request.notes())
                .recordedByEmployeeId(recordedByEmployeeId)
                .build();

        return toResponse(shiftHandoverRepository.save(handover));
    }

    public ShiftHandoverResponse getById(Long id) {
        return shiftHandoverRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shift handover not found: " + id));
    }

    // Null (not an exception) when nothing has ever been recorded - a fresh
    // venue with no handover history yet is a normal state, not an error.
    public ShiftHandoverResponse getLatest() {
        return shiftHandoverRepository.findFirstByOrderByShiftEndTimeDesc()
                .map(this::toResponse)
                .orElse(null);
    }

    public List<ShiftHandoverResponse> getAll() {
        return shiftHandoverRepository.findAllByOrderByShiftEndTimeDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ShiftHandoverResponse> getMyHistory(Long employeeId) {
        return shiftHandoverRepository.findByEmployeeIdOrderByShiftEndTimeDesc(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ShiftHandoverResponse toResponse(ShiftHandover handover) {
        String employeeName = employeeRepository.findById(handover.getEmployeeId())
                .map(Employee::getName)
                .orElse("Unknown");
        return ShiftHandoverResponse.fromEntity(handover, employeeName);
    }
}