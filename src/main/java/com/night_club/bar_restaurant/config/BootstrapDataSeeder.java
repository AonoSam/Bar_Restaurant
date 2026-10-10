package com.night_club.bar_restaurant.config;

import com.night_club.bar_restaurant.employee.Employee;
import com.night_club.bar_restaurant.employee.EmployeeRepository;
import com.night_club.bar_restaurant.employee.EmployeeStatus;
import com.night_club.bar_restaurant.employee.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the very first MANAGER account on startup, only if the employees
 * table is completely empty. Without this, there's no way to log in at all -
 * POST /api/employees requires MANAGER role, and no manager exists yet to
 * create one. Only runs once; on every later startup the table is non-empty
 * so this is a no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapDataSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.manager-username}")
    private String bootstrapUsername;

    @Value("${app.bootstrap.manager-password}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (employeeRepository.count() > 0) {
            return;
        }

        Employee manager = Employee.builder()
                .name("Default Manager")
                .username(bootstrapUsername)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .role(Role.MANAGER)
                .status(EmployeeStatus.ACTIVE)
                .build();

        employeeRepository.save(manager);

        log.warn("=================================================================");
        log.warn("No employees found - created bootstrap MANAGER account:");
        log.warn("  username: {}", bootstrapUsername);
        log.warn("  password: {}", bootstrapPassword);
        log.warn("CHANGE THIS PASSWORD (or create a real manager and disable this one)");
        log.warn("before this goes anywhere near production.");
        log.warn("=================================================================");
    }
}