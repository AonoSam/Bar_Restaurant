package com.night_club.bar_restaurant.websocket;

import com.night_club.bar_restaurant.auth.EmployeePrincipal;
import com.night_club.bar_restaurant.auth.EmployeeUserDetailsService;
import com.night_club.bar_restaurant.auth.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs on every inbound STOMP frame before it reaches the broker.
 *
 * SockJS's HTTP handshake (GET /ws/info, etc) does not reliably carry a
 * standard Authorization header the way a normal REST request does - the
 * browser's SockJS/STOMP client sends the JWT as a STOMP header on the
 * CONNECT frame instead, over the already-established socket. That's why
 * auth happens here rather than in JwtAuthFilter, which only ever sees the
 * initial HTTP handshake, not the STOMP frames that follow it.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final EmployeeUserDetailsService userDetailsService;

    private static final String COUNTER_TOPIC = "/topic/counter-orders";
    private static final String LOW_STOCK_TOPIC = "/topic/low-stock-alerts";
    private static final String NOTIFICATIONS_TOPIC = "/topic/notifications";

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization header on CONNECT frame");
        }

        String token = authHeader.substring(7);
        String username;

        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        EmployeePrincipal principal = (EmployeePrincipal) userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.isTokenValid(token, principal.getUsername()) || !principal.isEnabled()) {
            throw new IllegalArgumentException("Token is invalid, expired, or account is disabled");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        // Attaching the Authentication as the session's Principal is what makes
        // it available on every subsequent frame on this same connection
        // (including the SUBSCRIBE frames handled below), not just this one.
        accessor.setUser(authentication);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        if (destination.equals(COUNTER_TOPIC)) {
            requireRole(accessor, "Only COUNTER or MANAGER roles may subscribe to " + COUNTER_TOPIC,
                    "ROLE_COUNTER", "ROLE_MANAGER");
        } else if (destination.equals(LOW_STOCK_TOPIC)) {
            requireRole(accessor, "Only MANAGER may subscribe to " + LOW_STOCK_TOPIC,
                    "ROLE_MANAGER");
        } else if (destination.equals(NOTIFICATIONS_TOPIC)) {
            requireRole(accessor, "Only COUNTER or MANAGER roles may subscribe to " + NOTIFICATIONS_TOPIC,
                    "ROLE_COUNTER", "ROLE_MANAGER");
        }
        // no restriction on other destinations
    }

    private void requireRole(StompHeaderAccessor accessor, String errorMessage, String... allowedRoles) {
        Authentication authentication = (Authentication) accessor.getUser();

        if (authentication == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        boolean hasAllowedRole = authentication.getAuthorities().stream()
                .anyMatch(a -> List.of(allowedRoles).contains(a.getAuthority()));

        if (!hasAllowedRole) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}