package com.example.project.service;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.exception.DuplicateCodeException;
import com.example.project.exception.ProjectHasMembersException;
import com.example.project.exception.ProjectNotFoundException;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectMemberService memberService;

    /**
     * Get all projects with pagination and filtering
     */
    public PageResponse<ProjectDTO> getAllPaginated(
            ProjectStatus status, LocalDate from, LocalDate to,
            String code, String name, Pageable pageable) {

        log.debug("Fetching projects with filters - status: {}, from: {}, to: {}, code: {}, name: {}",
                 status, from, to, code, name);

        Page<Project> projectPage = projectRepository.findProjectsWithFilters(
            status, from, to, code, name, pageable);

        List<ProjectDTO> dtos = projectPage.getContent().stream()
            .map(this::convertToDTO)
            .toList();

        return PageResponse.<ProjectDTO>builder()
            .content(dtos)
            .page(projectPage.getNumber())
            .size(projectPage.getSize())
            .totalElements(projectPage.getTotalElements())
            .totalPages(projectPage.getTotalPages())
            .sort(pageable.getSort().toString())
            .first(projectPage.isFirst())
            .last(projectPage.isLast())
            .build();
    }

    /**
     * Get project by ID
     */
    public ProjectDTO getById(Long id) {
        log.debug("Fetching project with ID: {}", id);
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));
        return convertToDTO(project);
    }

    /**
     * Get project by code
     */
    public ProjectDTO getByCode(String code) {
        log.debug("Fetching project with code: {}", code);
        Project project = projectRepository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new ProjectNotFoundException("Project not found with code: " + code));
        return convertToDTO(project);
    }

    /**
     * Create new project
     */
    @Transactional
    public ProjectDTO create(ProjectCreateRequest request) {
        log.info("Creating new project with code: {}", request.getCode());

        // Check for duplicate code
        if (projectRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw DuplicateCodeException.forCode(request.getCode());
        }

        Project project = Project.builder()
            .code(request.getCode())
            .name(request.getName())
            .description(request.getDescription())
            .status(request.getStatus())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .build();

        Project saved = projectRepository.save(project);
        log.info("Created project with ID: {} and code: {}", saved.getId(), saved.getCode());

        return convertToDTO(saved);
    }

    /**
     * Update project (full replacement)
     */
    @Transactional
    public ProjectDTO update(Long id, ProjectUpdateRequest request) {
        log.info("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));

        // Check for duplicate code (excluding current project)
        if (!project.getCode().equalsIgnoreCase(request.getCode()) &&
            projectRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), id)) {
            throw DuplicateCodeException.forCode(request.getCode());
        }

        project.setCode(request.getCode());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        Project updated = projectRepository.save(project);
        log.info("Updated project with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Patch project (partial update)
     */
    @Transactional
    public ProjectDTO patch(Long id, ProjectPatchRequest request) {
        log.info("Patching project with ID: {}", id);

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));

        boolean updated = false;

        if (StringUtils.hasText(request.getCode()) && !project.getCode().equalsIgnoreCase(request.getCode())) {
            // Check for duplicate code (excluding current project)
            if (projectRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), id)) {
                throw DuplicateCodeException.forCode(request.getCode());
            }
            project.setCode(request.getCode());
            updated = true;
        }

        if (StringUtils.hasText(request.getName())) {
            project.setName(request.getName());
            updated = true;
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
            updated = true;
        }

        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
            updated = true;
        }

        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
            updated = true;
        }

        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
            updated = true;
        }

        if (updated) {
            Project patched = projectRepository.save(project);
            log.info("Patched project with ID: {}", patched.getId());
            return convertToDTO(patched);
        } else {
            log.debug("No changes needed for project with ID: {}", id);
            return convertToDTO(project);
        }
    }

    /**
     * Delete project
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting project with ID: {}", id);

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));

        // Check if project has members
        long memberCount = memberRepository.countByProjectId(id);
        if (memberCount > 0) {
            throw ProjectHasMembersException.forProject(id, (int) memberCount);
        }

        projectRepository.delete(project);
        log.info("Deleted project with ID: {}", id);
    }

    /**
     * Get project statistics
     */
    public ProjectStatsDTO getStats(String groupBy) {
        log.debug("Generating project statistics grouped by: {}", groupBy);

        long totalProjects = projectRepository.count();
        Map<String, Long> countByStatus = new HashMap<>();
        Map<String, Long> countByMonth = new HashMap<>();

        // Always get count by status
        List<Object[]> statusCounts = projectRepository.countByStatus();
        for (Object[] row : statusCounts) {
            ProjectStatus status = (ProjectStatus) row[0];
            Long count = (Long) row[1];
            countByStatus.put(status.name(), count);
        }

        // Conditionally get count by month
        if ("month".equalsIgnoreCase(groupBy)) {
            List<Object[]> monthCounts = projectRepository.countByMonth();
            for (Object[] row : monthCounts) {
                String month = (String) row[0];
                Long count = (Long) row[1];
                countByMonth.put(month, count);
            }
        }

        return ProjectStatsDTO.builder()
            .totalProjects(totalProjects)
            .countByStatus(countByStatus)
            .countByMonth(countByMonth.isEmpty() ? null : countByMonth)
            .build();
    }

    /**
     * Create Pageable with sorting
     */
    public Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    /**
     * Convert Project entity to DTO
     */
    private ProjectDTO convertToDTO(Project project) {
        List<ProjectMemberDTO> memberDTOs = memberService.getMembersByProjectId(project.getId(), false);

        return ProjectDTO.builder()
            .id(project.getId())
            .code(project.getCode())
            .name(project.getName())
            .description(project.getDescription())
            .status(project.getStatus())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .members(memberDTOs)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }
}