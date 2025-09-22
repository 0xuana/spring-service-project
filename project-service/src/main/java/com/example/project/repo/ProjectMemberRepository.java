package com.example.project.repo;

import com.example.project.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /**
     * Find all members of a specific project
     */
    List<ProjectMember> findByProjectId(Long projectId);

    /**
     * Find member by project and employee ID
     */
    Optional<ProjectMember> findByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    /**
     * Check if member exists for project and employee
     */
    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    /**
     * Find all projects for a specific employee
     */
    List<ProjectMember> findByEmployeeId(Long employeeId);

    /**
     * Count members in a project
     */
    long countByProjectId(Long projectId);

    /**
     * Delete member by project and employee ID
     */
    void deleteByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    /**
     * Find all members with their project details for a specific employee
     */
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.project WHERE pm.employeeId = :employeeId")
    List<ProjectMember> findByEmployeeIdWithProject(@Param("employeeId") Long employeeId);

    /**
     * Get total allocation percentage for an employee across all projects
     */
    @Query("SELECT COALESCE(SUM(pm.allocationPercent), 0) FROM ProjectMember pm WHERE pm.employeeId = :employeeId")
    Integer getTotalAllocationByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Find members by role (case-insensitive)
     */
    @Query("SELECT pm FROM ProjectMember pm WHERE UPPER(pm.role) LIKE UPPER(CONCAT('%', :role, '%'))")
    List<ProjectMember> findByRoleContainingIgnoreCase(@Param("role") String role);

    /**
     * Find members with allocation above threshold
     */
    List<ProjectMember> findByAllocationPercentGreaterThan(Integer threshold);

    /**
     * Find members with allocation below threshold
     */
    List<ProjectMember> findByAllocationPercentLessThan(Integer threshold);
}