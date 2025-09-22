package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to partially update a project")
public class ProjectPatchRequest {

    @Pattern(regexp = "^[A-Z0-9-]{3,20}$", message = "Project code must be 3-20 characters, uppercase letters, digits, and hyphens only")
    @Schema(description = "Project code (unique identifier)", example = "EMP-MGMT")
    private String code;

    @Size(min = 3, max = 120, message = "Project name must be between 3 and 120 characters")
    @Schema(description = "Project name", example = "Employee Management System")
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Schema(description = "Project description", example = "A comprehensive system for managing employee data")
    private String description;

    @Schema(description = "Project status")
    private ProjectStatus status;

    @Schema(description = "Project start date", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "Project end date (optional)", example = "2024-12-31")
    private LocalDate endDate;
}