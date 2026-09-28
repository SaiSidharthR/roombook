package com.example.roombook.dto;

import com.example.roombook.model.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EmployeeUpdateRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String department,
        @NotNull EmployeeRole role,
        @Pattern(regexp = "^$|.{8,}", message = "Password must be at least 8 characters") String password) {
}