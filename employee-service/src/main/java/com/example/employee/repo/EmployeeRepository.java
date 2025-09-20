package com.example.employee.repo;

import com.example.employee.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // Filtering methods
    Page<Employee> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Employee> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    List<Employee> findByDepartmentId(Long departmentId);

    // Combined filtering
    @Query("SELECT e FROM Employee e WHERE " +
           "(:email IS NULL OR LOWER(e.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:departmentId IS NULL OR e.departmentId = :departmentId)")
    Page<Employee> findWithFilters(@Param("email") String email,
                                  @Param("lastName") String lastName,
                                  @Param("departmentId") Long departmentId,
                                  Pageable pageable);

    // Search methods
    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchByNameOrEmail(@Param("searchTerm") String searchTerm);

    // Statistics
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.departmentId = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT e.departmentId, COUNT(e) FROM Employee e GROUP BY e.departmentId")
    List<Object[]> countByDepartmentIdGrouped();
}
