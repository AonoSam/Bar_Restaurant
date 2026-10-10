package com.night_club.bar_restaurant.payment;

import com.night_club.bar_restaurant.common.PhoneUtil;
import com.night_club.bar_restaurant.notification.NotificationService;
import com.night_club.bar_restaurant.notification.NotificationType;
import com.night_club.bar_restaurant.order.Order;
import com.night_club.bar_restaurant.order.OrderPaidEvent;
import com.night_club.bar_restaurant.order.OrderRepository;
import com.night_club.bar_restaurant.order.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DarajaService {

    private final RestClient darajaRestClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private String fetchAccessToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((consumerKey + ":" + consumerSecret).getBytes());

        DarajaTokenResponse response;
        try {
            response = darajaRestClient.get()
                    .uri("/oauth/v1/generate?grant_type=client_credentials")
                    .header("Authorization", "Basic " + credentials)
                    .retrieve()
                    .body(DarajaTokenResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Daraja OAuth token request failed: {}", e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not authenticate with M-Pesa - check mpesa.consumer-key/consumer-secret");
        }

        if (response == null || response.accessToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to obtain Daraja access token");
        }

        return response.accessToken();
    }

    @Transactional
    public StkPushResponse initiateStkPush(Order order, String rawPhoneNumber) {
        String phoneNumber;
        try {
            phoneNumber = PhoneUtil.normalize(rawPhoneNumber);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        String accessToken = fetchAccessToken();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = Base64.getEncoder()
                .encodeToString((shortcode + passkey + timestamp).getBytes());

        long amount = order.getTotal().longValue();

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", shortcode);
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("TransactionType", "CustomerPayBillOnline");
        payload.put("Amount", amount);
        payload.put("PartyA", phoneNumber);
        payload.put("PartyB", shortcode);
        payload.put("PhoneNumber", phoneNumber);
        payload.put("CallBackURL", callbackUrl);
        payload.put("AccountReference", order.getOrderNumber());
        payload.put("TransactionDesc", "Payment for order " + order.getOrderNumber());

        DarajaStkPushApiResponse response;
        try {
            response = darajaRestClient.post()
                    .uri("/mpesa/stkpush/v1/processrequest")
                    .header("Authorization", "Bearer " + accessToken)
                    .body(payload)
                    .retrieve()
                    .body(DarajaStkPushApiResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Daraja STK push request failed: {}", e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "STK push request to M-Pesa failed - check shortcode/passkey/callback-url configuration");
        }

        if (response == null || !"0".equals(response.responseCode())) {
            String desc = response != null ? response.responseDescription() : "no response from Daraja";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "STK push failed: " + desc);
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .phoneNumber(phoneNumber)
                .amount(order.getTotal())
                .merchantRequestId(response.merchantRequestId())
                .checkoutRequestId(response.checkoutRequestId())
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        return new StkPushResponse(
                response.merchantRequestId(),
                response.checkoutRequestId(),
                response.customerMessage()
        );
    }

    @Transactional
    public void handleCallback(DarajaCallbackPayload payload) {
        DarajaCallbackPayload.StkCallback callback = payload.body().stkCallback();
        String checkoutRequestId = callback.checkoutRequestId();

        Payment payment = paymentRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElse(null);

        if (payment == null) {
            log.warn("Received Daraja callback for unknown checkoutRequestId: {}", checkoutRequestId);
            return;
        }

        if (payment.getResultCode() != null) {
            log.info("Duplicate Daraja callback for checkoutRequestId {} - already processed, ignoring", checkoutRequestId);
            return;
        }

        payment.setResultCode(callback.resultCode());
        payment.setResultDesc(callback.resultDesc());

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found for payment: " + payment.getOrderId()));

        if (callback.isSuccess()) {
            String mpesaReceipt = extractStringValue(callback, "MpesaReceiptNumber");
            payment.setMpesaReceipt(mpesaReceipt);
            payment.setTransactionTime(LocalDateTime.now());
            payment.setStatus(PaymentStatus.PAID);

            order.setPaymentStatus(PaymentStatus.PAID);

            eventPublisher.publishEvent(new OrderPaidEvent(order.getId()));
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setPaymentStatus(PaymentStatus.FAILED);

            notificationService.create(
                    NotificationType.PAYMENT_FAILED,
                    "M-Pesa payment failed",
                    order.getOrderNumber() + " - " + callback.resultDesc(),
                    order.getId()
            );
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
    }

    private String extractStringValue(DarajaCallbackPayload.StkCallback callback, String name) {
        if (callback.callbackMetadata() == null || callback.callbackMetadata().item() == null) {
            return null;
        }
        return callback.callbackMetadata().item().stream()
                .filter(item -> name.equals(item.name()))
                .map(item -> String.valueOf(item.value()))
                .findFirst()
                .orElse(null);
    }
}