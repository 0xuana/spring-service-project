package com.example.employee.web;

import com.example.employee.dto.*;
import com.example.employee.service.EmployeeService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee API", description = "Production-grade Employee Management API")
public class EmployeeController {

    private final EmployeeService service;

    @Operation(summary = "Get all employees with pagination, sorting, and filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employees retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public PageResponse<EmployeeDTO> getAllEmployees(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort criteria (field,direction)", example = "lastName,asc")
            @RequestParam(defaultValue = "id,asc") String sort,

            @Parameter(description = "Filter by email (contains)", example = "john")
            @RequestParam(required = false) String email,

            @Parameter(description = "Filter by last name (contains)", example = "doe")
            @RequestParam(required = false) String lastName,

            @Parameter(description = "Filter by department ID", example = "1")
            @RequestParam(required = false) Long departmentId) {

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        return service.getAllPaginated(email, lastName, departmentId, pageable);
    }

    @Operation(summary = "Get employee by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee found"),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public EmployeeDTO getEmployeeById(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Include department details", example = "true")
            @RequestParam(defaultValue = "true") boolean enrich) {
        return service.getById(id, enrich);
    }

    @Operation(summary = "Create a new employee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Employee created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDTO createEmployee(
            @Valid @RequestBody EmployeeDTO dto,

            @Parameter(description = "Idempotency key for safe retries")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Creating employee with email: {}, idempotency key: {}", dto.getEmail(), idempotencyKey);
        return service.create(dto, idempotencyKey);
    }

    @Operation(summary = "Update employee (full update)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public EmployeeDTO updateEmployee(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Partially update employee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    public EmployeeDTO patchEmployee(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EmployeePatchRequest request) {
        return service.patch(id, request);
    }

    @Operation(summary = "Delete employee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Employee deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Search employees by name or email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public List<EmployeeDTO> searchEmployees(
            @Parameter(description = "Search term to match against name or email", example = "john")
            @RequestParam String q) {
        return service.search(q);
    }

    @Operation(summary = "Get employee statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public EmployeeStatsDTO getEmployeeStats() {
        return service.getStats();
    }

    @Operation(summary = "Get employees by department ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employees retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    @GetMapping("/by-department/{departmentId}")
    public List<EmployeeDTO> getEmployeesByDepartment(
            @Parameter(description = "Department ID", example = "1")
            @PathVariable Long departmentId) {
        return service.getByDepartmentId(departmentId);
    }

    @Operation(summary = "Get employee count by department ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    @GetMapping("/count-by-department/{departmentId}")
    public Long getEmployeeCountByDepartment(
            @Parameter(description = "Department ID", example = "1")
            @PathVariable Long departmentId) {
        return service.getCountByDepartmentId(departmentId);
    }

    // Backward compatibility - keep the old simple endpoints
    @GetMapping("/all")
    @Deprecated
    public List<EmployeeDTO> getAllEmployeesSimple() {
        return service.getAll();
    }
}
