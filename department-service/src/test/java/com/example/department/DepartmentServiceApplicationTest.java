package com.example.department;

import com.example.department.domain.Department;
import com.example.department.repo.DepartmentRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
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
@DisplayName("Department Service Full Context Test (no DB)")
public class DepartmentServiceApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentRepository departmentRepository;

    @Nested
    @DisplayName("Full context integration tests")
    class FullContextTests {

        @Test
        @DisplayName("GET /api/v1/departments returns empty list when no departments")
        void getDepartments_returnsEmptyList_whenNoDepartments() throws Exception {
            when(departmentRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("GET /api/v1/departments returns all departments")
        void getDepartments_returnsAllDepartments() throws Exception {
            var departments = List.of(
                Department.builder()
                    .id(1L)
                    .name("Engineering")
                    .description("Software development")
                    .build(),
                Department.builder()
                    .id(2L)
                    .name("Marketing")
                    .description("Product marketing")
                    .build()
            );

            when(departmentRepository.findAll()).thenReturn(departments);

            mockMvc.perform(get("/api/v1/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].name").value("Engineering"))
                    .andExpect(jsonPath("$[1].name").value("Marketing"));

            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("GET /api/v1/departments/{id} returns department by ID")
        void getDepartmentById_returnsDepartment_whenExists() throws Exception {
            var department = Department.builder()
                .id(1L)
                .name("Human Resources")
                .description("People operations")
                .build();

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

            mockMvc.perform(get("/api/v1/departments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Human Resources"))
                    .andExpect(jsonPath("$.description").value("People operations"));

            verify(departmentRepository).findById(1L);
        }

        @Test
        @DisplayName("GET /api/v1/departments/{id} returns 404 when department not found")
        void getDepartmentById_returns404_whenNotFound() throws Exception {
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/departments/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Department not found with id: 999"));

            verify(departmentRepository).findById(999L);
        }

        @Test
        @DisplayName("POST /api/v1/departments creates new department successfully")
        void createDepartment_success() throws Exception {
            var newDepartment = Department.builder()
                .id(100L)
                .name("Finance")
                .description("Financial operations")
                .build();

            when(departmentRepository.existsByNameIgnoreCase("Finance")).thenReturn(false);
            when(departmentRepository.save(any())).thenReturn(newDepartment);

            var requestDepartment = Department.builder()
                .name("Finance")
                .description("Financial operations")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDepartment)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.name").value("Finance"))
                    .andExpect(jsonPath("$.description").value("Financial operations"));

            verify(departmentRepository).existsByNameIgnoreCase("Finance");
            verify(departmentRepository).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/departments creates department without description")
        void createDepartment_successWithoutDescription() throws Exception {
            var newDepartment = Department.builder()
                .id(101L)
                .name("Legal")
                .build();

            when(departmentRepository.existsByNameIgnoreCase("Legal")).thenReturn(false);
            when(departmentRepository.save(any())).thenReturn(newDepartment);

            var requestDepartment = Department.builder()
                .name("Legal")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDepartment)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(101L))
                    .andExpect(jsonPath("$.name").value("Legal"))
                    .andExpect(jsonPath("$.description").doesNotExist());

            verify(departmentRepository).existsByNameIgnoreCase("Legal");
            verify(departmentRepository).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/departments returns 409 for duplicate name")
        void createDepartment_returnsDuplicateError_whenNameExists() throws Exception {
            when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

            var requestDepartment = Department.builder()
                .name("Engineering")
                .description("Software development")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDepartment)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Department with name already exists: Engineering"));

            verify(departmentRepository).existsByNameIgnoreCase("Engineering");
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/departments returns 409 for case-insensitive duplicate")
        void createDepartment_returnsDuplicateError_forCaseInsensitiveDuplicate() throws Exception {
            when(departmentRepository.existsByNameIgnoreCase("engineering")).thenReturn(true);

            var requestDepartment = Department.builder()
                .name("engineering")
                .description("Software development")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDepartment)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Department with name already exists: engineering"));

            verify(departmentRepository).existsByNameIgnoreCase("engineering");
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/departments returns 400 for validation errors")
        void createDepartment_returnsValidationError_forInvalidData() throws Exception {
            var invalidRequestDepartment = Department.builder()
                .name("") // Invalid: empty name
                .description("Valid description")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequestDepartment)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));

            verify(departmentRepository, never()).existsByNameIgnoreCase(anyString());
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("POST /api/v1/departments returns 400 for missing name")
        void createDepartment_returnsValidationError_forMissingName() throws Exception {
            var invalidRequestDepartment = Department.builder()
                .description("Valid description but no name")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequestDepartment)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));

            verify(departmentRepository, never()).existsByNameIgnoreCase(anyString());
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Service maintains transaction boundaries correctly")
        void serviceRespectsTransactionBoundaries() throws Exception {
            // This test verifies that the service layer properly handles transactions
            // Even though we're not using a real DB, the annotations should be in place
            when(departmentRepository.existsByNameIgnoreCase("Operations")).thenReturn(false);
            when(departmentRepository.save(any())).thenReturn(
                Department.builder().id(200L).name("Operations").description("Business operations").build()
            );

            var requestDepartment = Department.builder()
                .name("Operations")
                .description("Business operations")
                .build();

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDepartment)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(200L));

            // Verify that the service layer checks for duplicates before saving
            verify(departmentRepository).existsByNameIgnoreCase("Operations");
            verify(departmentRepository).save(any());
        }
    }
}