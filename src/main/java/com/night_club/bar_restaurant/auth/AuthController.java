package com.night_club.bar_restaurant.auth;

import com.night_club.bar_restaurant.employee.Employee;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        EmployeePrincipal principal;
        try {
            var authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            principal = (EmployeePrincipal) authResult.getPrincipal();
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Employee employee = principal.getEmployee();

        String token = jwtUtil.generateToken(
                employee.getUsername(),
                Map.of(
                        "role", employee.getRole().name(),
                        "employeeId", employee.getId(),
                        "name", employee.getName()
                )
        );

        LoginResponse response = new LoginResponse(
                token,
                employee.getId(),
                employee.getName(),
                employee.getUsername(),
                employee.getRole().name()
        );

        return ResponseEntity.ok(response);
    }
}
