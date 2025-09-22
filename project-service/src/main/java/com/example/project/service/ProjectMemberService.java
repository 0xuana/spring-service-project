package com.example.project.service;

import com.example.project.client.EmployeeClient;
import com.example.project.domain.Project;
import com.example.project.domain.ProjectMember;
import com.example.project.dto.*;
import com.example.project.exception.DuplicateMemberException;
import com.example.project.exception.EmployeeNotFoundException;
import com.example.project.exception.ProjectNotFoundException;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeClient employeeClient;

    /**
     * Get members by project ID with optional enrichment
     */
    public List<ProjectMemberDTO> getMembersByProjectId(Long projectId, boolean enrich) {
        log.debug("Fetching members for project ID: {}, enrich: {}", projectId, enrich);

        List<ProjectMember> members = memberRepository.findByProjectId(projectId);

        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProjectMemberDTO> memberDTOs = members.stream()
            .map(this::convertToDTO)
            .toList();

        if (enrich) {
            enrichWithEmployeeData(memberDTOs);
        }

        return memberDTOs;
    }

    /**
     * Add multiple members to a project
     */
    @Transactional
    public AddMembersResponse addMembers(Long projectId, AddMembersRequest request) {
        log.info("Adding {} members to project ID: {}", request.getMembers().size(), projectId);

        // Verify project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<MemberOperationResult> results = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        for (AddMemberRequest memberRequest : request.getMembers()) {
            try {
                ProjectMemberDTO member = addSingleMember(project, memberRequest);
                results.add(MemberOperationResult.success(member));
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to add member with employee ID {}: {}",
                        memberRequest.getEmployeeId(), e.getMessage());
                results.add(MemberOperationResult.error(memberRequest.getEmployeeId(), e.getMessage()));
                errorCount++;
            }
        }

        log.info("Added members to project {}: {} successful, {} failed",
                projectId, successCount, errorCount);

        return AddMembersResponse.builder()
            .totalProcessed(request.getMembers().size())
            .successCount(successCount)
            .errorCount(errorCount)
            .results(results)
            .build();
    }

    /**
     * Add single member to project
     */
    @Transactional
    public ProjectMemberDTO addMember(Long projectId, AddMemberRequest request) {
        log.info("Adding member with employee ID {} to project {}", request.getEmployeeId(), projectId);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return addSingleMember(project, request);
    }

    /**
     * Remove member from project
     */
    @Transactional
    public void removeMember(Long projectId, Long employeeId) {
        log.info("Removing employee {} from project {}", employeeId, projectId);

        // Verify project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        // Verify member exists
        ProjectMember member = memberRepository.findByProjectIdAndEmployeeId(projectId, employeeId)
            .orElseThrow(() -> new RuntimeException(
                "Employee " + employeeId + " is not a member of project " + projectId));

        memberRepository.delete(member);
        log.info("Removed employee {} from project {}", employeeId, projectId);
    }

    /**
     * Get member by project and employee ID
     */
    public ProjectMemberDTO getMember(Long projectId, Long employeeId, boolean enrich) {
        log.debug("Fetching member for project {} and employee {}", projectId, employeeId);

        ProjectMember member = memberRepository.findByProjectIdAndEmployeeId(projectId, employeeId)
            .orElseThrow(() -> new RuntimeException(
                "Employee " + employeeId + " is not a member of project " + projectId));

        ProjectMemberDTO dto = convertToDTO(member);

        if (enrich) {
            enrichWithEmployeeData(List.of(dto));
        }

        return dto;
    }

    /**
     * Add single member to project (internal method)
     */
    private ProjectMemberDTO addSingleMember(Project project, AddMemberRequest request) {
        Long employeeId = request.getEmployeeId();

        // Check if employee exists via Employee Service
        EmployeeDTO employee = null;
        try {
            employee = employeeClient.getEmployeeById(employeeId, false);
            if (employee == null) {
                throw EmployeeNotFoundException.forId(employeeId);
            }
        } catch (Exception e) {
            log.error("Failed to validate employee existence for ID {}: {}", employeeId, e.getMessage());
            throw EmployeeNotFoundException.forId(employeeId);
        }

        // Check if employee is already a member
        if (memberRepository.existsByProjectIdAndEmployeeId(project.getId(), employeeId)) {
            throw DuplicateMemberException.forProjectAndEmployee(project.getId(), employeeId);
        }

        // Create new member
        ProjectMember member = ProjectMember.builder()
            .project(project)
            .employeeId(employeeId)
            .role(request.getRole())
            .allocationPercent(request.getAllocationPercent())
            .build();

        ProjectMember saved = memberRepository.save(member);
        log.info("Added member with ID {} to project {}", saved.getId(), project.getId());

        return convertToDTO(saved);
    }

    /**
     * Enrich member DTOs with employee data
     */
    private void enrichWithEmployeeData(List<ProjectMemberDTO> memberDTOs) {
        if (memberDTOs.isEmpty()) {
            return;
        }

        List<Long> employeeIds = memberDTOs.stream()
            .map(ProjectMemberDTO::getEmployeeId)
            .toList();

        try {
            List<EmployeeDTO> employees = employeeClient.getEmployeesByIds(employeeIds);

            Map<Long, EmployeeDTO> employeeMap = employees.stream()
                .collect(Collectors.toMap(EmployeeDTO::getId, emp -> emp));

            memberDTOs.forEach(member -> {
                EmployeeDTO employee = employeeMap.get(member.getEmployeeId());
                if (employee != null) {
                    member.setEmployee(EmployeeDTO.builder()
                        .id(employee.getId())
                        .firstName(employee.getFirstName())
                        .lastName(employee.getLastName())
                        .email(employee.getEmail())
                        .build());
                }
            });
        } catch (Exception e) {
            log.warn("Failed to enrich member data with employee information: {}", e.getMessage());
        }
    }

    /**
     * Convert ProjectMember entity to DTO
     */
    private ProjectMemberDTO convertToDTO(ProjectMember member) {
        return ProjectMemberDTO.builder()
            .id(member.getId())
            .projectId(member.getProject().getId())
            .employeeId(member.getEmployeeId())
            .role(member.getRole())
            .allocationPercent(member.getAllocationPercent())
            .assignedAt(member.getAssignedAt())
            .build();
    }
}