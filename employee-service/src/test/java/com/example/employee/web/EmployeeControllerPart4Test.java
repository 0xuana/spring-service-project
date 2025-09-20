package com.example.employee.web;

import com.example.employee.dto.*;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = EmployeeController.class,
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import="
    }
)
@ActiveProfiles("test")
@DisplayName("Employee Controller Part 4 - Enhanced API Tests")
class EmployeeControllerPart4Test {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /employees with pagination should return paginated response")
    void testGetEmployeesWithPagination() throws Exception {
        // Given
        EmployeeDTO employee1 = EmployeeDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        PageResponse<EmployeeDTO> pageResponse = PageResponse.<EmployeeDTO>builder()
                .content(List.of(employee1))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .sort("id: ASC")
                .build();

        when(employeeService.getAllPaginated(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/employees")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /employees with filters should apply filters correctly")
    void testGetEmployeesWithFilters() throws Exception {
        // Given
        PageResponse<EmployeeDTO> pageResponse = PageResponse.<EmployeeDTO>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0L)
                .totalPages(0)
                .first(true)
                .last(true)
                .sort("id: ASC")
                .build();

        when(employeeService.getAllPaginated(eq("john"), eq("doe"), eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/employees")
                        .param("email", "john")
                        .param("lastName", "doe")
                        .param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("PUT /employees/{id} should update employee successfully")
    void testUpdateEmployee() throws Exception {
        // Given
        Long employeeId = 1L;
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();

        EmployeeDTO updatedEmployee = EmployeeDTO.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();

        when(employeeService.update(eq(employeeId), any(EmployeeUpdateRequest.class)))
                .thenReturn(updatedEmployee);

        // When & Then
        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("PATCH /employees/{id} should partially update employee")
    void testPatchEmployee() throws Exception {
        // Given
        Long employeeId = 1L;
        EmployeePatchRequest request = EmployeePatchRequest.builder()
                .firstName("Jane")
                .build();

        EmployeeDTO patchedEmployee = EmployeeDTO.builder()
                .id(employeeId)
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .departmentId(1L)
                .build();

        when(employeeService.patch(eq(employeeId), any(EmployeePatchRequest.class)))
                .thenReturn(patchedEmployee);

        // When & Then
        mockMvc.perform(patch("/api/v1/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @DisplayName("DELETE /employees/{id} should delete employee successfully")
    void testDeleteEmployee() throws Exception {
        // Given
        Long employeeId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /employees/search should return search results")
    void testSearchEmployees() throws Exception {
        // Given
        String searchTerm = "john";
        EmployeeDTO employee = EmployeeDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(employeeService.search(eq(searchTerm)))
                .thenReturn(List.of(employee));

        // When & Then
        mockMvc.perform(get("/api/v1/employees/search")
                        .param("q", searchTerm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    @DisplayName("GET /employees/stats should return employee statistics")
    void testGetEmployeeStats() throws Exception {
        // Given
        EmployeeStatsDTO.DepartmentCount deptCount = EmployeeStatsDTO.DepartmentCount.builder()
                .departmentId(1L)
                .count(5L)
                .departmentName("Engineering")
                .build();

        EmployeeStatsDTO stats = EmployeeStatsDTO.builder()
                .totalEmployees(10L)
                .countsByDepartment(List.of(deptCount))
                .build();

        when(employeeService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/v1/employees/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(10))
                .andExpect(jsonPath("$.countsByDepartment").isArray())
                .andExpect(jsonPath("$.countsByDepartment.length()").value(1))
                .andExpect(jsonPath("$.countsByDepartment[0].departmentId").value(1))
                .andExpect(jsonPath("$.countsByDepartment[0].count").value(5))
                .andExpect(jsonPath("$.countsByDepartment[0].departmentName").value("Engineering"));
    }

    @Test
    @DisplayName("POST /employees with Idempotency-Key should handle idempotent requests")
    void testCreateEmployeeWithIdempotencyKey() throws Exception {
        // Given
        String idempotencyKey = "unique-key-123";
        EmployeeDTO requestDto = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        EmployeeDTO responseDto = EmployeeDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        when(employeeService.create(any(EmployeeDTO.class), eq(idempotencyKey)))
                .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/employees")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("GET /employees/{id} with enrich=false should return employee without department")
    void testGetEmployeeByIdWithoutEnrichment() throws Exception {
        // Given
        Long employeeId = 1L;
        EmployeeDTO employee = EmployeeDTO.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        when(employeeService.getById(eq(employeeId), eq(false)))
                .thenReturn(employee);

        // When & Then
        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .param("enrich", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.department").doesNotExist());
    }
}