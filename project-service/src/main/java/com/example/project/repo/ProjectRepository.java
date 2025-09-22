package com.example.project.repo;

import com.example.project.domain.Project;
import com.example.project.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find project by code (case-insensitive)
     */
    Optional<Project> findByCodeIgnoreCase(String code);

    /**
     * Check if project exists with given code (excluding specific ID for updates)
     */
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /**
     * Check if project exists with given code
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Find projects with filtering
     */
    @Query("SELECT p FROM Project p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:from IS NULL OR p.startDate >= :from) AND " +
           "(:to IS NULL OR p.endDate <= :to) AND " +
           "(:code IS NULL OR UPPER(p.code) = UPPER(:code)) AND " +
           "(:name IS NULL OR UPPER(p.name) LIKE UPPER(CONCAT('%', :name, '%')))")
    Page<Project> findProjectsWithFilters(
        @Param("status") ProjectStatus status,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("code") String code,
        @Param("name") String name,
        Pageable pageable
    );

    /**
     * Count projects by status
     */
    @Query("SELECT p.status as status, COUNT(p) as count FROM Project p GROUP BY p.status")
    List<Object[]> countByStatus();

    /**
     * Count projects by month based on start date
     */
    @Query("SELECT CONCAT(YEAR(p.startDate), '-', FORMAT(MONTH(p.startDate), '00')) as month, COUNT(p) as count " +
           "FROM Project p GROUP BY YEAR(p.startDate), MONTH(p.startDate) ORDER BY month")
    List<Object[]> countByMonth();

    /**
     * Find projects by status
     */
    List<Project> findByStatus(ProjectStatus status);

    /**
     * Find projects with start date in range
     */
    List<Project> findByStartDateBetween(LocalDate start, LocalDate end);

    /**
     * Find projects with end date before given date
     */
    List<Project> findByEndDateBefore(LocalDate date);

    /**
     * Find projects with end date after given date
     */
    List<Project> findByEndDateAfter(LocalDate date);
}