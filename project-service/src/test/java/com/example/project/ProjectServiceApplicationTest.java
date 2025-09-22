package com.example.project;

import com.example.project.client.EmployeeClient;
import com.example.project.domain.Project;
import com.example.project.domain.ProjectMember;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Project Service Application Integration Tests")
class ProjectServiceApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private ProjectMemberRepository memberRepository;

    @MockBean
    private EmployeeClient employeeClient;

    private Project testProject;
    private ProjectMember testMember;
    private EmployeeDTO testEmployee;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
            .id(1L)
            .code("TEST-001")
            .name("Test Project")
            .description("Integration test project")
            .status(ProjectStatus.ACTIVE)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        testMember = ProjectMember.builder()
            .id(1L)
            .project(testProject)
            .employeeId(101L)
            .role("Developer")
            .allocationPercent(80)
            .assignedAt(LocalDateTime.now())
            .build();

        testEmployee = EmployeeDTO.builder()
            .id(101L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@company.com")
            .build();
    }

    @Nested
    @DisplayName("Project Management Integration Tests")
    class ProjectManagementTests {

        @Test
        @DisplayName("Should create project successfully through full stack")
        void shouldCreateProjectSuccessfullyThroughFullStack() throws Exception {
            // Given
            ProjectCreateRequest createRequest = ProjectCreateRequest.builder()
                .code("NEW-001")
                .name("New Integration Project")
                .description("Created through integration test")
                .status(ProjectStatus.PLANNED)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

            when(projectRepository.existsByCodeIgnoreCase("NEW-001")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("TEST-001"))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should handle duplicate code error through full stack")
        void shouldHandleDuplicateCodeErrorThroughFullStack() throws Exception {
            // Given
            ProjectCreateRequest createRequest = ProjectCreateRequest.builder()
                .code("DUPLICATE")
                .name("Duplicate Project")
                .status(ProjectStatus.PLANNED)
                .startDate(LocalDate.of(2024, 1, 1))
                .build();

            when(projectRepository.existsByCodeIgnoreCase("DUPLICATE")).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.company.com/problems/duplicate-code"))
                .andExpect(jsonPath("$.title").value("Duplicate Project Code"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Project with code 'DUPLICATE' already exists"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.instance").exists());
        }

        @Test
        @DisplayName("Should retrieve project with members through full stack")
        void shouldRetrieveProjectWithMembersThroughFullStack() throws Exception {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of(testMember));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("TEST-001"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members[0].employeeId").value(101))
                .andExpect(jsonPath("$.members[0].role").value("Developer"))
                .andExpect(jsonPath("$.members[0].allocationPercent").value(80));
        }

        @Test
        @DisplayName("Should handle project not found through full stack")
        void shouldHandleProjectNotFoundThroughFullStack() throws Exception {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.company.com/problems/not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Project not found with id: 999"));
        }
    }

    @Nested
    @DisplayName("Project Member Management Integration Tests")
    class ProjectMemberManagementTests {

        @Test
        @DisplayName("Should add member successfully through full stack")
        void shouldAddMemberSuccessfullyThroughFullStack() throws Exception {
            // Given
            AddMemberRequest addRequest = AddMemberRequest.builder()
                .employeeId(101L)
                .role("Developer")
                .allocationPercent(80)
                .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(testEmployee);
            when(memberRepository.existsByProjectIdAndEmployeeId(1L, 101L)).thenReturn(false);
            when(memberRepository.save(any(ProjectMember.class))).thenReturn(testMember);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(101))
                .andExpect(jsonPath("$.role").value("Developer"))
                .andExpect(jsonPath("$.allocationPercent").value(80))
                .andExpect(jsonPath("$.projectId").value(1));
        }

        @Test
        @DisplayName("Should handle employee not found through full stack")
        void shouldHandleEmployeeNotFoundThroughFullStack() throws Exception {
            // Given
            AddMemberRequest addRequest = AddMemberRequest.builder()
                .employeeId(999L)
                .role("Developer")
                .allocationPercent(80)
                .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(999L, false)).thenReturn(null);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.company.com/problems/employee-not-found"))
                .andExpect(jsonPath("$.title").value("Referenced Employee Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Employee not found with id: 999"));
        }

        @Test
        @DisplayName("Should handle duplicate member through full stack")
        void shouldHandleDuplicateMemberThroughFullStack() throws Exception {
            // Given
            AddMemberRequest addRequest = AddMemberRequest.builder()
                .employeeId(101L)
                .role("Developer")
                .allocationPercent(80)
                .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(testEmployee);
            when(memberRepository.existsByProjectIdAndEmployeeId(1L, 101L)).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://api.company.com/problems/duplicate-member"))
                .andExpect(jsonPath("$.title").value("Duplicate Project Member"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Employee 101 is already a member of project 1"));
        }

        @Test
        @DisplayName("Should retrieve enriched members through full stack")
        void shouldRetrieveEnrichedMembersThroughFullStack() throws Exception {
            // Given
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of(testMember));
            when(employeeClient.getEmployeesByIds(List.of(101L))).thenReturn(List.of(testEmployee));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members")
                    .param("enrich", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employeeId").value(101))
                .andExpect(jsonPath("$[0].employee.firstName").value("John"))
                .andExpect(jsonPath("$[0].employee.lastName").value("Doe"))
                .andExpect(jsonPath("$[0].employee.email").value("john.doe@company.com"));
        }

        @Test
        @DisplayName("Should handle employee service failure gracefully")
        void shouldHandleEmployeeServiceFailureGracefully() throws Exception {
            // Given
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of(testMember));
            when(employeeClient.getEmployeesByIds(any())).thenThrow(new RuntimeException("Service unavailable"));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members")
                    .param("enrich", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employeeId").value(101))
                .andExpect(jsonPath("$[0].employee").doesNotExist()); // Enrichment failed, but basic data still returned
        }
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should validate project date range through full stack")
        void shouldValidateProjectDateRangeThroughFullStack() throws Exception {
            // Given
            ProjectCreateRequest invalidRequest = ProjectCreateRequest.builder()
                .code("INVALID-DATES")
                .name("Invalid Date Project")
                .status(ProjectStatus.PLANNED)
                .startDate(LocalDate.of(2024, 12, 31))
                .endDate(LocalDate.of(2024, 1, 1)) // End before start
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.company.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should validate member allocation percentage through full stack")
        void shouldValidateMemberAllocationPercentageThroughFullStack() throws Exception {
            // Given
            AddMemberRequest invalidRequest = AddMemberRequest.builder()
                .employeeId(101L)
                .role("Developer")
                .allocationPercent(150) // Invalid: > 100
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").isArray());
        }
    }
}