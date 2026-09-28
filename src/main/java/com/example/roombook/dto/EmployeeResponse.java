package com.example.roombook.dto;

import com.example.roombook.model.Employee;
import com.example.roombook.model.EmployeeRole;

public record EmployeeResponse(Long id, String name, String email, String department, EmployeeRole role) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getName(), employee.getEmail(),
                employee.getDepartment(), employee.getRole());
    }
}