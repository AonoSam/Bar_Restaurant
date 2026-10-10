package com.night_club.bar_restaurant.report;

import com.night_club.bar_restaurant.employee.Employee;
import com.night_club.bar_restaurant.employee.EmployeeRepository;
import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.order.PaymentStatus;
import com.night_club.bar_restaurant.payment.Payment;
import com.night_club.bar_restaurant.payment.PaymentMethod;
import com.night_club.bar_restaurant.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;

    public ShiftReportResponse getShiftReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Order> paidOrders = orderRepository.findByPaymentStatusAndCreatedAtBetween(
                PaymentStatus.PAID, start, end
        );

        BigDecimal totalRevenue = paidOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> paidPayments = paymentRepository.findByStatusAndCreatedAtBetween(
                PaymentStatus.PAID, start, end
        );

        BigDecimal cashTotal = sumByMethod(paidPayments, PaymentMethod.CASH);
        BigDecimal mpesaTotal = sumByMethod(paidPayments, PaymentMethod.MPESA);

        return new ShiftReportResponse(date, paidOrders.size(), totalRevenue, cashTotal, mpesaTotal);
    }

    public List<WaiterSalesResponse> getWaiterSales(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Order> paidOrders = orderRepository.findByPaymentStatusAndCreatedAtBetween(
                PaymentStatus.PAID, start, end
        );

        Map<Long, List<Order>> byWaiter = paidOrders.stream()
                .collect(Collectors.groupingBy(Order::getWaiterId));

        return byWaiter.entrySet().stream()
                .map(entry -> {
                    Long waiterId = entry.getKey();
                    List<Order> orders = entry.getValue();

                    BigDecimal total = orders.stream()
                            .map(Order::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Waiter may have been deleted/deactivated since these orders were
                    // placed - don't let a missing employee record break the report.
                    String waiterName = employeeRepository.findById(waiterId)
                            .map(Employee::getName)
                            .orElse("Unknown (id " + waiterId + ")");

                    return new WaiterSalesResponse(waiterId, waiterName, orders.size(), total);
                })
                .sorted(Comparator.comparing(WaiterSalesResponse::totalSales).reversed())
                .toList();
    }

    private BigDecimal sumByMethod(List<Payment> payments, PaymentMethod method) {
        return payments.stream()
                .filter(p -> p.getPaymentMethod() == method)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Self-service sales for one specific waiter, for TODAY only. Deliberately
     * takes no date parameter and always uses LocalDate.now() - this is the
     * self-service endpoint any employee can call for their own numbers, so
     * it must be structurally incapable of returning historical or another
     * employee's data, not just policy-restricted to it.
     */
    public MySalesResponse getMySales(Long waiterId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Order> myPaidOrders = orderRepository.findByPaymentStatusAndCreatedAtBetween(
                        PaymentStatus.PAID, start, end
                ).stream()
                .filter(order -> order.getWaiterId().equals(waiterId))
                .toList();

        BigDecimal totalSales = myPaidOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = myPaidOrders.isEmpty()
                ? BigDecimal.ZERO
                : totalSales.divide(BigDecimal.valueOf(myPaidOrders.size()), 2, RoundingMode.HALF_UP);

        return new MySalesResponse(today, myPaidOrders.size(), totalSales, averageOrderValue);
    }

    /**
     * CSV export of the full shift report for a given date - manager-only.
     * Built by hand rather than pulling in a CSV library, since the shape is
     * simple: a summary block followed by a per-waiter breakdown table.
     */
    public String generateShiftCsv(LocalDate date) {
        ShiftReportResponse shift = getShiftReport(date);
        List<WaiterSalesResponse> waiterSales = getWaiterSales(date);

        StringBuilder csv = new StringBuilder();
        csv.append("Shift Report - ").append(date).append("\n\n");
        csv.append("Total Orders,Total Revenue,Cash Total,M-Pesa Total\n");
        csv.append(shift.totalOrders()).append(",")
                .append(shift.totalRevenue()).append(",")
                .append(shift.cashTotal()).append(",")
                .append(shift.mpesaTotal()).append("\n\n");

        csv.append("Waiter,Order Count,Total Sales\n");
        for (WaiterSalesResponse w : waiterSales) {
            // Waiter names could theoretically contain a comma - wrap in quotes
            // to keep the CSV well-formed rather than assuming they never will.
            csv.append("\"").append(w.waiterName().replace("\"", "\"\"")).append("\",")
                    .append(w.orderCount()).append(",")
                    .append(w.totalSales()).append("\n");
        }

        return csv.toString();
    }
}