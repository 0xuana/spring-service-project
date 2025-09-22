package com.example.project.dto;

import com.example.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project details with members")
public class ProjectDTO {

    @Schema(description = "Project ID", example = "1")
    private Long id;

    @Schema(description = "Project code (unique identifier)", example = "EMP-MGMT")
    private String code;

    @Schema(description = "Project name", example = "Employee Management System")
    private String name;

    @Schema(description = "Project description", example = "A comprehensive system for managing employee data")
    private String description;

    @Schema(description = "Project status")
    private ProjectStatus status;

    @Schema(description = "Project start date", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "Project end date", example = "2024-12-31")
    private LocalDate endDate;

    @Schema(description = "Project members")
    private List<ProjectMemberDTO> members;

    @Schema(description = "Project creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Project last update timestamp")
    private LocalDateTime updatedAt;
}