package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add a member to a project")
public class AddMemberRequest {

    @NotNull(message = "Employee ID is required")
    @Schema(description = "Employee ID", example = "1")
    private Long employeeId;

    @NotBlank(message = "Role is required")
    @Size(min = 2, max = 60, message = "Role must be between 2 and 60 characters")
    @Schema(description = "Member role in project", example = "Developer")
    private String role;

    @NotNull(message = "Allocation percentage is required")
    @Min(value = 0, message = "Allocation percentage must be between 0 and 100")
    @Max(value = 100, message = "Allocation percentage must be between 0 and 100")
    @Schema(description = "Allocation percentage (0-100)", example = "75")
    private Integer allocationPercent;
}