package com.example.department.repo;

import com.example.department.domain.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.StringUtils;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<Department> findByCodeIgnoreCase(String code);

    @Query("SELECT d FROM Department d WHERE " +
           "(:name IS NULL OR :name = '' OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:code IS NULL OR :code = '' OR LOWER(d.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<Department> findWithFilters(@Param("name") String name,
                                   @Param("code") String code,
                                   Pageable pageable);

    @Query("SELECT d FROM Department d WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Department> searchByNameCodeOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);

    default Page<Department> findAllWithFilters(String name, String code, Pageable pageable) {
        if (StringUtils.hasText(name) || StringUtils.hasText(code)) {
            return findWithFilters(name, code, pageable);
        }
        return findAll(pageable);
    }
}
