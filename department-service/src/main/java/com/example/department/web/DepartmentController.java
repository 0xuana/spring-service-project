package com.example.department.web;

import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentPatchRequest;
import com.example.department.dto.DepartmentUpdateRequest;
import com.example.department.dto.EmployeeDTO;
import com.example.department.dto.PageResponse;
import com.example.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Department API", description = "Comprehensive department management operations")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @Operation(
        summary = "Get all departments with pagination and filtering",
        description = "Retrieve departments with optional pagination, sorting, and filtering by name and code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Departments retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public PageResponse<DepartmentDTO> getAllDepartments(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort specification (field,direction)", example = "name,asc")
            @RequestParam(defaultValue = "id,asc") String sort,

            @Parameter(description = "Filter by department name (partial match)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filter by department code (partial match)")
            @RequestParam(required = false) String code) {

        Pageable pageable = service.createPageable(page, size, sort);
        return service.getAllPaginated(name, code, pageable);
    }

    @Operation(
        summary = "Search departments",
        description = "Search departments by name, code, or description"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/search")
    public PageResponse<DepartmentDTO> searchDepartments(
            @Parameter(description = "Search term to match against name, code, or description")
            @RequestParam String q,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort specification (field,direction)", example = "name,asc")
            @RequestParam(defaultValue = "id,asc") String sort) {

        Pageable pageable = service.createPageable(page, size, sort);
        return service.searchDepartments(q, pageable);
    }

    @Operation(
        summary = "Get department by ID",
        description = "Retrieve a specific department by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department found"),
        @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public DepartmentDTO getDepartmentById(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id) {
        return service.getByIdAsDTO(id);
    }

    @Operation(
        summary = "Get department by code",
        description = "Retrieve a department by its unique code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department found"),
        @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/by-code/{code}")
    public DepartmentDTO getDepartmentByCode(
            @Parameter(description = "Department code", required = true)
            @PathVariable String code) {
        return service.getByCode(code);
    }

    @Operation(
        summary = "Get employees in department",
        description = "Retrieve all employees belonging to a specific department"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employees retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}/employees")
    public List<EmployeeDTO> getDepartmentEmployees(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id) {
        return service.getEmployeesByDepartmentId(id);
    }

    @Operation(
        summary = "Create new department",
        description = "Create a new department with validation for unique name and code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Department created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate name/code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentDTO createDepartment(
            @Parameter(description = "Department data", required = true)
            @Valid @RequestBody DepartmentDTO departmentDTO) {
        return service.createFromDTO(departmentDTO);
    }

    @Operation(
        summary = "Update department (full replacement)",
        description = "Completely replace department data with new values"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate name/code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public DepartmentDTO updateDepartment(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Updated department data", required = true)
            @Valid @RequestBody DepartmentUpdateRequest updateRequest) {
        return service.updateById(id, updateRequest);
    }

    @Operation(
        summary = "Partially update department",
        description = "Update only the provided fields of a department"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Department updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate name/code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    public DepartmentDTO patchDepartment(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "Fields to update", required = true)
            @Valid @RequestBody DepartmentPatchRequest patchRequest) {
        return service.patchById(id, patchRequest);
    }

    @Operation(
        summary = "Delete department",
        description = "Delete a department. Fails if employees are still assigned to the department."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Department deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Department has assigned employees",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id) {
        service.deleteById(id);
    }

    @Operation(
        summary = "Legacy endpoint - Get all departments",
        description = "Legacy endpoint that returns simple list of departments without pagination",
        deprecated = true
    )
    @GetMapping("/legacy")
    @Deprecated
    public List<Department> getAllDepartmentsLegacy() {
        return service.getAll();
    }
}
