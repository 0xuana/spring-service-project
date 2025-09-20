package com.example.department.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentUpdateRequest {
    @NotBlank(message = "code is required")
    @Size(min = 2, max = 20, message = "code must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "code must contain only uppercase letters, digits, and hyphens")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    private String name;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    @Email(message = "managerEmail must be a valid email address")
    @Size(max = 200, message = "managerEmail must not exceed 200 characters")
    private String managerEmail;

    @Size(max = 100, message = "location must not exceed 100 characters")
    private String location;
}