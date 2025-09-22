package com.example.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for bulk creating employees")
public class BulkCreateRequestDTO {

    @NotEmpty(message = "Employee list cannot be empty")
    @Size(max = 100, message = "Cannot create more than 100 employees at once")
    @Valid
    @Schema(description = "List of employees to create", example = "[{...}]")
    private List<EmployeeDTO> employees;
}