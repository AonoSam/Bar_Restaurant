package com.night_club.bar_restaurant.auth;

public record LoginResponse(
        String token,
        Long employeeId,
        String name,
        String username,
        String role
) {
}
