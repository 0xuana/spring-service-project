package com.example.employee;

import com.example.employee.client.DepartmentClient;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.repo.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import="
    }
)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Employee Service Full Context Test (no DB)")
public class EmployeeServiceApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private DepartmentClient departmentClient;

    @Nested
    @DisplayName("Full context integration tests")
    class FullContextTests {

        @Test
        @DisplayName("GET /api/v1/employees returns empty list when no employees")
        void getEmployees_returnsEmptyList_whenNoEmployees() throws Exception {
            when(employeeRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            verify(employeeRepository).findAll();
        }

        @Test
        @DisplayName("GET /api/v1/employees returns employees with department enrichment")
        void getEmployees_returnsEmployeesWithDepartments() throws Exception {
            var employee = com.example.employee.domain.Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(10L)
                .build();

            var department = DepartmentDTO.builder()
                .id(10L)
                .name("Engineering")
                .description("Software development")
                .build();

            when(employeeRepository.findAll()).thenReturn(List.of(employee));
            when(departmentClient.getDepartment(10L)).thenReturn(department);

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].firstName").value("John"))
                    .andExpect(jsonPath("$[0].department.name").value("Engineering"));

            verify(employeeRepository).findAll();
            verify(departmentClient).getDepartment(10L);
        }

        @Test
        @DisplayName("GET /api/v1/employees/{id} returns employee by ID")
        void getEmployeeById_returnsEmployee_whenExists() throws Exception {
            var employee = com.example.employee.domain.Employee.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .departmentId(5L)
                .build();

            var department = DepartmentDTO.builder()
                .id(5L)
                .name("Marketing")
                .build();

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(departmentClient.getDepartment(5L)).thenReturn(department);

            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.department.name").value("Marketing"));

            verify(employeeRepository).findById(1L);
            verify(departmentClient).getDepartment(5L);
        }

        @Test
        @DisplayName("GET /api/v1/employees/{id} returns 404 when employee not found")
        void getEmployeeById_returns404_whenNotFound() throws Exception {
            when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/employees/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Employee not found with id: 999"));

            verify(employeeRepository).findById(999L);
            verify(departmentClient, never()).getDepartment(anyLong());
        }

        @Test
        @DisplayName("POST /api/v1/employees creates new employee successfully")
        void createEmployee_success() throws Exception {
            var newEmployee = com.example.employee.domain.Employee.builder()
                .id(100L)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice@example.com")
                .departmentId(3L)
                .build();

            when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(employeeRepository.save(any())).thenReturn(newEmployee);

            var requestDto = EmployeeDTO.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice@example.com")
                .departmentId(3L)
                .build();

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.firstName").value("Alice"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"));

            verify(employeeRepository).existsByEmail("alice@example.com");
            verify(employeeRepository).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/employees returns 409 for duplicate email")
        void createEmployee_returnsDuplicateError_whenEmailExists() throws Exception {
            when(employeeRepository.existsByEmail("existing@example.com")).thenReturn(true);

            var requestDto = EmployeeDTO.builder()
                .firstName("Bob")
                .lastName("Wilson")
                .email("existing@example.com")
                .build();

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Employee with email already exists: existing@example.com"));

            verify(employeeRepository).existsByEmail("existing@example.com");
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/employees returns 400 for validation errors")
        void createEmployee_returnsValidationError_forInvalidData() throws Exception {
            var invalidRequestDto = EmployeeDTO.builder()
                .firstName("") // Invalid: empty firstName
                .lastName("Smith")
                .email("invalid-email") // Invalid: malformed email
                .build();

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));

            verify(employeeRepository, never()).existsByEmail(anyString());
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Service handles department client failures gracefully")
        void serviceHandlesDepartmentClientFailures() throws Exception {
            var employee = com.example.employee.domain.Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(10L)
                .build();

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(departmentClient.getDepartment(10L)).thenThrow(new RuntimeException("Department service unavailable"));

            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.department").doesNotExist()); // Should be null due to graceful handling

            verify(employeeRepository).findById(1L);
            verify(departmentClient).getDepartment(10L);
        }
    }
}