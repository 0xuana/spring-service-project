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
@Schema(description = "Result of adding a member to a project")
public class MemberOperationResult {

    @Schema(description = "Employee ID", example = "1")
    private Long employeeId;

    @Schema(description = "Success status", example = "true")
    private boolean success;

    @Schema(description = "Error message if operation failed", example = "Employee already exists in project")
    private String message;

    @Schema(description = "Created member details (if successful)")
    private ProjectMemberDTO member;

    public static MemberOperationResult success(ProjectMemberDTO member) {
        return MemberOperationResult.builder()
            .employeeId(member.getEmployeeId())
            .success(true)
            .message("Member added successfully")
            .member(member)
            .build();
    }

    public static MemberOperationResult error(Long employeeId, String message) {
        return MemberOperationResult.builder()
            .employeeId(employeeId)
            .success(false)
            .message(message)
            .build();
    }
}