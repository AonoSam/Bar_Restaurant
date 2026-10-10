package com.night_club.bar_restaurant.report;

import com.night_club.bar_restaurant.employee.Employee;
import com.night_club.bar_restaurant.employee.EmployeeRepository;
import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderItem;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.order.PaymentStatus;
import com.night_club.bar_restaurant.payment.Payment;
import com.night_club.bar_restaurant.payment.PaymentMethod;
import com.night_club.bar_restaurant.payment.PaymentRepository;
import com.night_club.bar_restaurant.product.Product;
import com.night_club.bar_restaurant.product.ProductCategory;
import com.night_club.bar_restaurant.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;

    private static final int TOP_N = 5;

    // Read-only transaction kept open for the whole method - Order.items is a
    // lazy @OneToMany, and this method iterates it across many orders while
    // building the top-products breakdown. Without this, that access would
    // throw LazyInitializationException outside the original query's session.
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate today = LocalDate.now();

        BigDecimal todaySales = sumOrders(paidOrdersBetween(today, today));
        long todayOrders = paidOrdersBetween(today, today).size();
        BigDecimal weekSales = sumOrders(paidOrdersBetween(today.minusDays(6), today));
        BigDecimal monthSales = sumOrders(paidOrdersBetween(today.withDayOfMonth(1), today));

        List<Order> rangeOrders = paidOrdersBetween(rangeStart, rangeEnd);

        return new DashboardResponse(
                todaySales,
                todayOrders,
                weekSales,
                monthSales,
                rangeStart,
                rangeEnd,
                topProductsByCategory(rangeOrders, ProductCategory.DRINK),
                topProductsByCategory(rangeOrders, ProductCategory.FOOD),
                busiestHours(rangeOrders),
                paymentDistribution(rangeStart, rangeEnd),
                topWaiters(rangeOrders)
        );
    }

    private List<Order> paidOrdersBetween(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        return orderRepository.findByPaymentStatusAndCreatedAtBetween(PaymentStatus.PAID, startDateTime, endDateTime);
    }

    private BigDecimal sumOrders(List<Order> orders) {
        return orders.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Aggregated in Java rather than a JPQL join query - OrderItem doesn't
     * store product category directly, and for a single-venue POS's order
     * volume this is simple, correct, and fast enough. Would be worth moving
     * to a database-level aggregate query if this ever needs to scale past
     * one venue's monthly order volume.
     */
    private List<TopProductResponse> topProductsByCategory(List<Order> orders, ProductCategory category) {
        Set<Long> productIds = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, long[]> quantityByProduct = new HashMap<>();
        Map<Long, BigDecimal> revenueByProduct = new HashMap<>();
        Map<Long, String> nameByProduct = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Product product = productsById.get(item.getProductId());
                if (product == null || product.getCategory() != category) continue;

                quantityByProduct.merge(item.getProductId(), new long[]{item.getQuantity()},
                        (a, b) -> new long[]{a[0] + b[0]});
                revenueByProduct.merge(item.getProductId(), item.getTotal(), BigDecimal::add);
                nameByProduct.putIfAbsent(item.getProductId(), item.getProductName());
            }
        }

        return quantityByProduct.entrySet().stream()
                .map(entry -> new TopProductResponse(
                        entry.getKey(),
                        nameByProduct.get(entry.getKey()),
                        entry.getValue()[0],
                        revenueByProduct.get(entry.getKey())
                ))
                .sorted(Comparator.comparingLong(TopProductResponse::quantitySold).reversed())
                .limit(TOP_N)
                .toList();
    }

    private List<HourlySalesResponse> busiestHours(List<Order> orders) {
        Map<Integer, long[]> countByHour = new HashMap<>();
        Map<Integer, BigDecimal> revenueByHour = new HashMap<>();

        for (Order order : orders) {
            int hour = order.getCreatedAt().getHour();
            countByHour.merge(hour, new long[]{1}, (a, b) -> new long[]{a[0] + b[0]});
            revenueByHour.merge(hour, order.getTotal(), BigDecimal::add);
        }

        List<HourlySalesResponse> result = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            result.add(new HourlySalesResponse(
                    hour,
                    countByHour.getOrDefault(hour, new long[]{0})[0],
                    revenueByHour.getOrDefault(hour, BigDecimal.ZERO)
            ));
        }
        return result;
    }

    private PaymentMethodBreakdownResponse paymentDistribution(LocalDate start, LocalDate end) {
        List<Payment> payments = paymentRepository.findByStatusAndCreatedAtBetween(
                PaymentStatus.PAID, start.atStartOfDay(), end.plusDays(1).atStartOfDay()
        );

        BigDecimal cashTotal = sumByMethod(payments, PaymentMethod.CASH);
        BigDecimal mpesaTotal = sumByMethod(payments, PaymentMethod.MPESA);
        BigDecimal combined = cashTotal.add(mpesaTotal);

        double cashPct = combined.signum() == 0 ? 0.0
                : cashTotal.divide(combined, 4, RoundingMode.HALF_UP).doubleValue() * 100;
        double mpesaPct = combined.signum() == 0 ? 0.0
                : mpesaTotal.divide(combined, 4, RoundingMode.HALF_UP).doubleValue() * 100;

        return new PaymentMethodBreakdownResponse(cashTotal, mpesaTotal, cashPct, mpesaPct);
    }

    private BigDecimal sumByMethod(List<Payment> payments, PaymentMethod method) {
        return payments.stream()
                .filter(p -> p.getPaymentMethod() == method)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<WaiterSalesResponse> topWaiters(List<Order> orders) {
        Map<Long, List<Order>> byWaiter = orders.stream().collect(Collectors.groupingBy(Order::getWaiterId));

        return byWaiter.entrySet().stream()
                .map(entry -> {
                    Long waiterId = entry.getKey();
                    List<Order> waiterOrders = entry.getValue();
                    BigDecimal total = sumOrders(waiterOrders);
                    String name = employeeRepository.findById(waiterId)
                            .map(Employee::getName)
                            .orElse("Unknown (id " + waiterId + ")");
                    return new WaiterSalesResponse(waiterId, name, waiterOrders.size(), total);
                })
                .sorted(Comparator.comparing(WaiterSalesResponse::totalSales).reversed())
                .limit(TOP_N)
                .toList();
    }
}