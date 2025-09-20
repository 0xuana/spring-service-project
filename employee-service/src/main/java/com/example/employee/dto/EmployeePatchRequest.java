package com.example.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePatchRequest {
    @Size(max = 120)
    private String firstName;

    @Size(max = 120)
    private String lastName;

    @Email(message = "email must be valid")
    @Size(max = 200)
    private String email;

    private Long departmentId;
}