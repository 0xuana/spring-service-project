package com.example.project.web;

import com.example.project.dto.*;
import com.example.project.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Member API", description = "Project member management operations")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    @Operation(
        summary = "List project members",
        description = "Retrieve all members of a specific project with optional employee enrichment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}/members")
    public List<ProjectMemberDTO> getProjectMembers(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Include employee details", example = "true")
            @RequestParam(defaultValue = "false") boolean enrich) {
        return memberService.getMembersByProjectId(id, enrich);
    }

    @Operation(
        summary = "Get specific project member",
        description = "Retrieve a specific member by project ID and employee ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member found"),
        @ApiResponse(responseCode = "404", description = "Project or member not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{projectId}/members/{employeeId}")
    public ProjectMemberDTO getProjectMember(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long projectId,

            @Parameter(description = "Employee ID", required = true)
            @PathVariable Long employeeId,

            @Parameter(description = "Include employee details", example = "true")
            @RequestParam(defaultValue = "false") boolean enrich) {
        return memberService.getMember(projectId, employeeId, enrich);
    }

    @Operation(
        summary = "Add member to project",
        description = "Add a single member to a project with employee validation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Member added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Project or employee not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Employee already a member",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberDTO addMember(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Member data", required = true)
            @Valid @RequestBody AddMemberRequest request) {
        return memberService.addMember(id, request);
    }

    @Operation(
        summary = "Add multiple members to project",
        description = "Add multiple members to a project with per-item results"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk operation completed (check individual results)"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/members/bulk")
    public AddMembersResponse addMembers(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Multiple members data", required = true)
            @Valid @RequestBody AddMembersRequest request) {
        return memberService.addMembers(id, request);
    }

    @Operation(
        summary = "Remove member from project",
        description = "Remove a specific member from a project"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Member removed successfully"),
        @ApiResponse(responseCode = "404", description = "Project, employee, or membership not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{projectId}/members/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long projectId,

            @Parameter(description = "Employee ID", required = true)
            @PathVariable Long employeeId) {
        memberService.removeMember(projectId, employeeId);
    }
}