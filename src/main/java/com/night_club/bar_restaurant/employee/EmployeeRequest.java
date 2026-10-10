package com.night_club.bar_restaurant.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Username is required") String username,
        // Only required on create - null/blank on update means "don't change the password"
        String password,
        @NotNull(message = "Role is required") Role role,
        String phone
) {
}