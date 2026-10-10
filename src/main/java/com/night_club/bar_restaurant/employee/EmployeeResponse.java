package com.night_club.bar_restaurant.employee;

import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String name,
        String username,
        Role role,
        String phone,
        EmployeeStatus status,
        LocalDateTime lastLoginAt,
        LocalDateTime lastLogoutAt,
        boolean online,

        boolean clockedIn
) {
    public static EmployeeResponse fromEntity(Employee employee, boolean clockedIn) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getUsername(),
                employee.getRole(),
                employee.getPhone(),
                employee.getStatus(),
                employee.getLastLoginAt(),
                employee.getLastLogoutAt(),
                isOnline(employee),
                clockedIn
        );
    }


    private static boolean isOnline(Employee employee) {
        LocalDateTime lastLogin = employee.getLastLoginAt();
        if (lastLogin == null) return false;
        LocalDateTime lastLogout = employee.getLastLogoutAt();
        return lastLogout == null || lastLogin.isAfter(lastLogout);
    }
}