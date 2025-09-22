package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project member details")
public class ProjectMemberDTO {

    @Schema(description = "Member ID", example = "1")
    private Long id;

    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Schema(description = "Employee ID", example = "1")
    private Long employeeId;

    @Schema(description = "Member role in project", example = "Developer")
    private String role;

    @Schema(description = "Allocation percentage (0-100)", example = "75")
    private Integer allocationPercent;

    @Schema(description = "Date and time when member was assigned to project")
    private LocalDateTime assignedAt;

    @Schema(description = "Employee details (only when enriched)")
    private EmployeeDTO employee;
}