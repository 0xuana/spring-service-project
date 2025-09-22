package com.example.project.web;

import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.service.ProjectService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project API", description = "Comprehensive project management operations")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
        summary = "List projects with filtering",
        description = "Retrieve projects with optional filtering by status, date range, code, and name"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projects retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public PageResponse<ProjectDTO> getAllProjects(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort specification (field,direction)", example = "name,asc")
            @RequestParam(defaultValue = "id,asc") String sort,

            @Parameter(description = "Filter by project status")
            @RequestParam(required = false) ProjectStatus status,

            @Parameter(description = "Filter projects with start date >= this date")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Filter projects with end date <= this date")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Filter by exact project code")
            @RequestParam(required = false) String code,

            @Parameter(description = "Filter by project name (partial match)")
            @RequestParam(required = false) String name) {

        Pageable pageable = projectService.createPageable(page, size, sort);
        return projectService.getAllPaginated(status, from, to, code, name, pageable);
    }

    @Operation(
        summary = "Get project by ID",
        description = "Retrieve a specific project by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project found"),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ProjectDTO getProjectById(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id) {
        return projectService.getById(id);
    }

    @Operation(
        summary = "Get project by code",
        description = "Retrieve a project by its unique code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project found"),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/by-code/{code}")
    public ProjectDTO getProjectByCode(
            @Parameter(description = "Project code", required = true)
            @PathVariable String code) {
        return projectService.getByCode(code);
    }

    @Operation(
        summary = "Create new project",
        description = "Create a new project with validation for unique code and valid date range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Project created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Project code already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDTO createProject(
            @Parameter(description = "Project data", required = true)
            @Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }

    @Operation(
        summary = "Update project (full replacement)",
        description = "Completely replace project data with new values"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Project code already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public ProjectDTO updateProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Updated project data", required = true)
            @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(id, request);
    }

    @Operation(
        summary = "Partially update project",
        description = "Update only the provided fields of a project"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Project code already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    public ProjectDTO patchProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Fields to update", required = true)
            @Valid @RequestBody ProjectPatchRequest request) {
        return projectService.patch(id, request);
    }

    @Operation(
        summary = "Delete project",
        description = "Delete a project. Fails if the project has active members."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Project has active members",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id) {
        projectService.delete(id);
    }

    @Operation(
        summary = "Get project statistics",
        description = "Get project counts by status and optionally by month"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public ProjectStatsDTO getProjectStats(
            @Parameter(description = "Group by option: 'status' (default) or 'month'")
            @RequestParam(defaultValue = "status") String groupBy) {
        return projectService.getStats(groupBy);
    }
}