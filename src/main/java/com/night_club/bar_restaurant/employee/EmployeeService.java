package com.night_club.bar_restaurant.employee;

import com.night_club.bar_restaurant.attendance.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRepository attendanceRepository;

    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll().stream()
                .map(e -> EmployeeResponse.fromEntity(e, isClockedIn(e.getId())))
                .toList();
    }

    public EmployeeResponse getById(Long id) {
        Employee employee = findOrThrow(id);
        return EmployeeResponse.fromEntity(employee, isClockedIn(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required when creating an employee");
        }
        if (employeeRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken: " + request.username());
        }

        Employee employee = Employee.builder()
                .name(request.name())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .phone(request.phone())
                .status(EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(employee);
        // Freshly created - can't possibly have an open attendance record yet.
        return EmployeeResponse.fromEntity(saved, false);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);

        // Username change checked against uniqueness only if it's actually changing
        if (!employee.getUsername().equals(request.username())
                && employeeRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken: " + request.username());
        }

        employee.setName(request.name());
        employee.setUsername(request.username());
        employee.setRole(request.role());
        employee.setPhone(request.phone());

        // Password only changes if a new one was actually provided - leaving it
        // blank on an edit form means "keep the existing password"
        if (request.password() != null && !request.password().isBlank()) {
            employee.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponse.fromEntity(saved, isClockedIn(id));
    }

    @Transactional
    public void deactivate(Long id) {
        Employee employee = findOrThrow(id);
        // Soft delete only - past orders reference waiterId, so a hard delete
        // would orphan historical order data, same reasoning as Product's
        // soft-delete-via-DISCONTINUED pattern.
        employee.setStatus(EmployeeStatus.DISABLED);
        employeeRepository.save(employee);
    }

    private boolean isClockedIn(Long employeeId) {
        return attendanceRepository.findByEmployeeIdAndClockOutTimeIsNull(employeeId).isPresent();
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + id));
    }
}