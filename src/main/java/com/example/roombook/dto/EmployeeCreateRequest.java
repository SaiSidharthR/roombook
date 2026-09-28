package com.example.roombook.dto;

import com.example.roombook.model.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String department,
        EmployeeRole role,
        @NotBlank @Size(min = 8) String password) {
}