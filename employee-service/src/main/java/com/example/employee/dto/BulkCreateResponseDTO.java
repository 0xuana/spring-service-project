package com.example.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for bulk create operation")
public class BulkCreateResponseDTO {

    @Schema(description = "Total number of employees processed", example = "10")
    private int totalProcessed;

    @Schema(description = "Number of employees successfully created", example = "8")
    private int successCount;

    @Schema(description = "Number of employees that failed to create", example = "2")
    private int failureCount;

    @Schema(description = "List of successfully created employees")
    private List<EmployeeDTO> successful;

    @Schema(description = "List of creation failures with reasons")
    private List<BulkCreateFailureDTO> failures;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Details about a failed employee creation")
    public static class BulkCreateFailureDTO {

        @Schema(description = "Index of the employee in the original request", example = "2")
        private int index;

        @Schema(description = "The employee data that failed to create")
        private EmployeeDTO employee;

        @Schema(description = "Reason for failure", example = "Email already exists")
        private String reason;

        @Schema(description = "Error code", example = "DUPLICATE_EMAIL")
        private String errorCode;
    }
}