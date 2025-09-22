package com.example.project.web;

import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.exception.DuplicateCodeException;
import com.example.project.exception.ProjectNotFoundException;
import com.example.project.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@DisplayName("ProjectController Web Slice Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    private ProjectDTO testProjectDTO;
    private ProjectCreateRequest createRequest;
    private ProjectUpdateRequest updateRequest;
    private ProjectPatchRequest patchRequest;
    private PageResponse<ProjectDTO> pageResponse;

    @BeforeEach
    void setUp() {
        testProjectDTO = ProjectDTO.builder()
            .id(1L)
            .code("TEST-001")
            .name("Test Project")
            .description("Test project description")
            .status(ProjectStatus.ACTIVE)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .members(List.of())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        createRequest = ProjectCreateRequest.builder()
            .code("NEW-001")
            .name("New Project")
            .description("New project description")
            .status(ProjectStatus.PLANNED)
            .startDate(LocalDate.of(2024, 6, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .build();

        updateRequest = ProjectUpdateRequest.builder()
            .code("UPD-001")
            .name("Updated Project")
            .description("Updated description")
            .status(ProjectStatus.ACTIVE)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 11, 30))
            .build();

        patchRequest = ProjectPatchRequest.builder()
            .status(ProjectStatus.ON_HOLD)
            .description("Patched description")
            .build();

        pageResponse = PageResponse.<ProjectDTO>builder()
            .content(List.of(testProjectDTO))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sort("id,asc")
            .first(true)
            .last(true)
            .build();
    }

    @Nested
    @DisplayName("GET /api/v1/projects Tests")
    class GetAllProjectsTests {

        @Test
        @DisplayName("Should return paginated projects")
        void shouldReturnPaginatedProjects() throws Exception {
            // Given
            when(projectService.createPageable(0, 20, "id,asc")).thenReturn(Pageable.unpaged());
            when(projectService.getAllPaginated(any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/projects")
                    .param("page", "0")
                    .param("size", "20")
                    .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].code").value("TEST-001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return projects with filters")
        void shouldReturnProjectsWithFilters() throws Exception {
            // Given
            when(projectService.createPageable(0, 20, "id,asc")).thenReturn(Pageable.unpaged());
            when(projectService.getAllPaginated(eq(ProjectStatus.ACTIVE), any(), any(), eq("TEST"), eq("Project"), any()))
                .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/projects")
                    .param("status", "ACTIVE")
                    .param("code", "TEST")
                    .param("name", "Project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].code").value("TEST-001"));
        }

        @Test
        @DisplayName("Should return empty page when no projects found")
        void shouldReturnEmptyPageWhenNoProjectsFound() throws Exception {
            // Given
            PageResponse<ProjectDTO> emptyPage = PageResponse.<ProjectDTO>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0L)
                .totalPages(0)
                .build();

            when(projectService.createPageable(0, 20, "id,asc")).thenReturn(Pageable.unpaged());
            when(projectService.getAllPaginated(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{id} Tests")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return project when found")
        void shouldReturnProjectWhenFound() throws Exception {
            // Given
            when(projectService.getById(1L)).thenReturn(testProjectDTO);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("TEST-001"))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectNotFound() throws Exception {
            // Given
            when(projectService.getById(999L)).thenThrow(new ProjectNotFoundException(999L));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/by-code/{code} Tests")
    class GetProjectByCodeTests {

        @Test
        @DisplayName("Should return project when found by code")
        void shouldReturnProjectWhenFoundByCode() throws Exception {
            // Given
            when(projectService.getByCode("TEST-001")).thenReturn(testProjectDTO);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/by-code/TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST-001"));
        }

        @Test
        @DisplayName("Should return 404 when project not found by code")
        void shouldReturn404WhenProjectNotFoundByCode() throws Exception {
            // Given
            when(projectService.getByCode("UNKNOWN")).thenThrow(new ProjectNotFoundException("Project not found with code: UNKNOWN"));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/by-code/UNKNOWN"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/projects Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project successfully")
        void shouldCreateProjectSuccessfully() throws Exception {
            // Given
            when(projectService.create(any(ProjectCreateRequest.class))).thenReturn(testProjectDTO);

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("TEST-001"));
        }

        @Test
        @DisplayName("Should return 400 for validation errors")
        void shouldReturn400ForValidationErrors() throws Exception {
            // Given
            ProjectCreateRequest invalidRequest = ProjectCreateRequest.builder()
                .code("") // Invalid: empty code
                .name("") // Invalid: empty name
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 1))
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should return 409 for duplicate code")
        void shouldReturn409ForDuplicateCode() throws Exception {
            // Given
            when(projectService.create(any())).thenThrow(DuplicateCodeException.forCode("NEW-001"));

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Duplicate Project Code"));
        }

        @Test
        @DisplayName("Should return 400 for invalid date range")
        void shouldReturn400ForInvalidDateRange() throws Exception {
            // Given
            ProjectCreateRequest invalidDateRequest = ProjectCreateRequest.builder()
                .code("VALID-001")
                .name("Valid Project")
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 12, 31))
                .endDate(LocalDate.of(2024, 1, 1)) // End date before start date
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/projects/{id} Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project successfully")
        void shouldUpdateProjectSuccessfully() throws Exception {
            // Given
            when(projectService.update(eq(1L), any(ProjectUpdateRequest.class))).thenReturn(testProjectDTO);

            // When & Then
            mockMvc.perform(put("/api/v1/projects/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Should return 404 when project not found for update")
        void shouldReturn404WhenProjectNotFoundForUpdate() throws Exception {
            // Given
            when(projectService.update(eq(999L), any())).thenThrow(new ProjectNotFoundException(999L));

            // When & Then
            mockMvc.perform(put("/api/v1/projects/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/projects/{id} Tests")
    class PatchProjectTests {

        @Test
        @DisplayName("Should patch project successfully")
        void shouldPatchProjectSuccessfully() throws Exception {
            // Given
            when(projectService.patch(eq(1L), any(ProjectPatchRequest.class))).thenReturn(testProjectDTO);

            // When & Then
            mockMvc.perform(patch("/api/v1/projects/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/projects/{id} Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project successfully")
        void shouldDeleteProjectSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when project not found for delete")
        void shouldReturn404WhenProjectNotFoundForDelete() throws Exception {
            // Given
            doThrow(new ProjectNotFoundException(999L)).when(projectService).delete(999L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/stats Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should return project statistics")
        void shouldReturnProjectStatistics() throws Exception {
            // Given
            ProjectStatsDTO stats = ProjectStatsDTO.builder()
                .totalProjects(10L)
                .countByStatus(java.util.Map.of("ACTIVE", 5L, "PLANNED", 3L, "COMPLETED", 2L))
                .build();

            when(projectService.getStats("status")).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/stats")
                    .param("groupBy", "status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(10))
                .andExpect(jsonPath("$.countByStatus.ACTIVE").value(5))
                .andExpect(jsonPath("$.countByStatus.PLANNED").value(3));
        }
    }
}