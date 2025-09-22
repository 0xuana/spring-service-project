package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee information for enriched project member data")
public class EmployeeDTO {

    @Schema(description = "Employee ID", example = "1")
    private Long id;

    @Schema(description = "Employee first name", example = "John")
    private String firstName;

    @Schema(description = "Employee last name", example = "Doe")
    private String lastName;

    @Schema(description = "Employee email", example = "john.doe@company.com")
    private String email;
}