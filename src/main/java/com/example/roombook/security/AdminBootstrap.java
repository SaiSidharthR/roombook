package com.example.roombook.security;

import com.example.roombook.model.Employee;
import com.example.roombook.model.EmployeeRole;
import com.example.roombook.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${roombook.bootstrap-admin.email:}") String email,
            @Value("${roombook.bootstrap-admin.password:}") String password) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)
                || employeeRepository.existsByEmail(email)) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
        }

        Employee admin = new Employee();
        admin.setName("RoomBook Administrator");
        admin.setEmail(email);
        admin.setDepartment("Administration");
        admin.setRole(EmployeeRole.ADMIN);
        admin.setPassword(passwordEncoder.encode(password));
        employeeRepository.save(admin);
    }
}