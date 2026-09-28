package com.example.roombook.service;

import com.example.roombook.exception.ResourceConflictException;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.model.Employee;
import com.example.roombook.repository.BookingRepository;
import com.example.roombook.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<Employee> listEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", email));
    }

    public Employee createEmployee(Employee employee) {
        employee.setId(null);
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new ResourceConflictException("Email address is already registered");
        }
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee employee = getEmployee(id);
        boolean emailChanged = !employee.getEmail().equalsIgnoreCase(updatedEmployee.getEmail());
        if (emailChanged && employeeRepository.existsByEmail(updatedEmployee.getEmail())) {
            throw new ResourceConflictException("Email address is already registered");
        }

        employee.setName(updatedEmployee.getName());
        employee.setEmail(updatedEmployee.getEmail());
        employee.setDepartment(updatedEmployee.getDepartment());
        employee.setRole(updatedEmployee.getRole());
        if (StringUtils.hasText(updatedEmployee.getPassword())) {
            employee.setPassword(passwordEncoder.encode(updatedEmployee.getPassword()));
        }
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployee(id);
        if (bookingRepository.existsByOrganizer_Id(id)) {
            throw new ResourceConflictException("Employee has booking history and cannot be deleted");
        }
        employeeRepository.delete(employee);
    }
}