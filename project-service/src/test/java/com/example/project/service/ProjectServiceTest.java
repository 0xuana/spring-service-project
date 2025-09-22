package com.example.project.service;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.exception.DuplicateCodeException;
import com.example.project.exception.ProjectHasMembersException;
import com.example.project.exception.ProjectNotFoundException;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository memberRepository;

    @Mock
    private ProjectMemberService memberService;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private ProjectCreateRequest createRequest;
    private ProjectUpdateRequest updateRequest;
    private ProjectPatchRequest patchRequest;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
            .id(1L)
            .code("TEST-001")
            .name("Test Project")
            .description("Test project description")
            .status(ProjectStatus.ACTIVE)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .members(new ArrayList<>())
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
    }

    @Nested
    @DisplayName("Get All Projects Tests")
    class GetAllProjectsTests {

        @Test
        @DisplayName("Should return paginated projects with filters")
        void shouldReturnPaginatedProjectsWithFilters() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Project> projectPage = new PageImpl<>(List.of(testProject), pageable, 1);
            List<ProjectMemberDTO> members = new ArrayList<>();

            when(projectRepository.findProjectsWithFilters(
                eq(ProjectStatus.ACTIVE), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(projectPage);
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(members);

            // When
            PageResponse<ProjectDTO> result = projectService.getAllPaginated(
                ProjectStatus.ACTIVE, null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);

            ProjectDTO projectDTO = result.getContent().get(0);
            assertThat(projectDTO.getId()).isEqualTo(1L);
            assertThat(projectDTO.getCode()).isEqualTo("TEST-001");
            assertThat(projectDTO.getName()).isEqualTo("Test Project");
        }

        @Test
        @DisplayName("Should return empty page when no projects found")
        void shouldReturnEmptyPageWhenNoProjectsFound() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Project> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(projectRepository.findProjectsWithFilters(any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(emptyPage);

            // When
            PageResponse<ProjectDTO> result = projectService.getAllPaginated(
                null, null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Get Project By ID Tests")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return project when found")
        void shouldReturnProjectWhenFound() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(new ArrayList<>());

            // When
            ProjectDTO result = projectService.getById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("TEST-001");
            assertThat(result.getName()).isEqualTo("Test Project");
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void shouldThrowExceptionWhenProjectNotFound() {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.getById(999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("Create Project Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project successfully")
        void shouldCreateProjectSuccessfully() {
            // Given
            when(projectRepository.existsByCodeIgnoreCase("NEW-001")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(new ArrayList<>());

            // When
            ProjectDTO result = projectService.create(createRequest);

            // Then
            assertThat(result).isNotNull();
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when code already exists")
        void shouldThrowExceptionWhenCodeAlreadyExists() {
            // Given
            when(projectRepository.existsByCodeIgnoreCase("NEW-001")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> projectService.create(createRequest))
                .isInstanceOf(DuplicateCodeException.class)
                .hasMessageContaining("NEW-001");

            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Project Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project successfully")
        void shouldUpdateProjectSuccessfully() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByCodeIgnoreCaseAndIdNot("UPD-001", 1L)).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(new ArrayList<>());

            // When
            ProjectDTO result = projectService.update(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when project not found for update")
        void shouldThrowExceptionWhenProjectNotFoundForUpdate() {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.update(999L, updateRequest))
                .isInstanceOf(ProjectNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when new code already exists")
        void shouldThrowExceptionWhenNewCodeAlreadyExists() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByCodeIgnoreCaseAndIdNot("UPD-001", 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> projectService.update(1L, updateRequest))
                .isInstanceOf(DuplicateCodeException.class);
        }
    }

    @Nested
    @DisplayName("Delete Project Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project successfully when no members")
        void shouldDeleteProjectSuccessfullyWhenNoMembers() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(memberRepository.countByProjectId(1L)).thenReturn(0L);

            // When
            projectService.delete(1L);

            // Then
            verify(projectRepository).delete(testProject);
        }

        @Test
        @DisplayName("Should throw exception when project has members")
        void shouldThrowExceptionWhenProjectHasMembers() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(memberRepository.countByProjectId(1L)).thenReturn(3L);

            // When & Then
            assertThatThrownBy(() -> projectService.delete(1L))
                .isInstanceOf(ProjectHasMembersException.class)
                .hasMessageContaining("3 active members");

            verify(projectRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when project not found for delete")
        void shouldThrowExceptionWhenProjectNotFoundForDelete() {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.delete(999L))
                .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Patch Project Tests")
    class PatchProjectTests {

        @Test
        @DisplayName("Should patch project successfully")
        void shouldPatchProjectSuccessfully() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(new ArrayList<>());

            // When
            ProjectDTO result = projectService.patch(1L, patchRequest);

            // Then
            assertThat(result).isNotNull();
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should return unchanged project when no updates needed")
        void shouldReturnUnchangedProjectWhenNoUpdatesNeeded() {
            // Given
            ProjectPatchRequest emptyPatch = ProjectPatchRequest.builder().build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(new ArrayList<>());

            // When
            ProjectDTO result = projectService.patch(1L, emptyPatch);

            // Then
            assertThat(result).isNotNull();
            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Project By Code Tests")
    class GetProjectByCodeTests {

        @Test
        @DisplayName("Should return project when found by code")
        void shouldReturnProjectWhenFoundByCode() {
            // Given
            when(projectRepository.findByCodeIgnoreCase("TEST-001")).thenReturn(Optional.of(testProject));
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(new ArrayList<>());

            // When
            ProjectDTO result = projectService.getByCode("TEST-001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("TEST-001");
        }

        @Test
        @DisplayName("Should throw exception when project not found by code")
        void shouldThrowExceptionWhenProjectNotFoundByCode() {
            // Given
            when(projectRepository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> projectService.getByCode("UNKNOWN"))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        }
    }
}