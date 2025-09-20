package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.*;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentClient departmentClient;

    // Simple in-memory cache for idempotency keys
    private final Map<String, EmployeeDTO> idempotencyCache = new ConcurrentHashMap<>();

    public List<EmployeeDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PageResponse<EmployeeDTO> getAllPaginated(String email, String lastName, Long departmentId, Pageable pageable) {
        Page<Employee> page;

        if (StringUtils.hasText(email) || StringUtils.hasText(lastName) || departmentId != null) {
            page = repository.findWithFilters(email, lastName, departmentId, pageable);
        } else {
            page = repository.findAll(pageable);
        }

        List<EmployeeDTO> content = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<EmployeeDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .sort(pageable.getSort().toString())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public EmployeeDTO getById(Long id) {
        Employee e = repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        return toDTO(e);
    }

    public EmployeeDTO getById(Long id, boolean enrichWithDepartment) {
        Employee e = repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        if (enrichWithDepartment) {
            return toDTO(e);
        } else {
            return toDTOWithoutDepartment(e);
        }
    }

    @Transactional
    public EmployeeDTO create(EmployeeDTO dto, String idempotencyKey) {
        // Check idempotency key if provided
        if (StringUtils.hasText(idempotencyKey)) {
            EmployeeDTO cached = idempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return cached;
            }
        }

        if (repository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEmailException(dto.getEmail());
        }
        Employee e = Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .departmentId(dto.getDepartmentId())
                .build();
        e = repository.save(e);
        EmployeeDTO result = toDTO(e);

        // Cache the result if idempotency key provided
        if (StringUtils.hasText(idempotencyKey)) {
            idempotencyCache.put(idempotencyKey, result);
        }

        return result;
    }

    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
        return create(dto, null);
    }

    @Transactional
    public EmployeeDTO update(Long id, EmployeeUpdateRequest request) {
        Employee existing = repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));

        // Check for email uniqueness if email is being changed
        if (!existing.getEmail().equals(request.getEmail()) &&
            repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateEmailException(request.getEmail());
        }

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setEmail(request.getEmail());
        existing.setDepartmentId(request.getDepartmentId());

        Employee updated = repository.save(existing);
        return toDTO(updated);
    }

    @Transactional
    public EmployeeDTO patch(Long id, EmployeePatchRequest request) {
        Employee existing = repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));

        if (StringUtils.hasText(request.getFirstName())) {
            existing.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            existing.setLastName(request.getLastName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            // Check for email uniqueness if email is being changed
            if (!existing.getEmail().equals(request.getEmail()) &&
                repository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateEmailException(request.getEmail());
            }
            existing.setEmail(request.getEmail());
        }
        if (request.getDepartmentId() != null) {
            existing.setDepartmentId(request.getDepartmentId());
        }

        Employee updated = repository.save(existing);
        return toDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        repository.deleteById(id);
    }

    public List<EmployeeDTO> search(String searchTerm) {
        return repository.searchByNameOrEmail(searchTerm).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EmployeeStatsDTO getStats() {
        long totalEmployees = repository.count();
        List<Object[]> departmentCounts = repository.countByDepartmentIdGrouped();

        List<EmployeeStatsDTO.DepartmentCount> counts = departmentCounts.stream()
                .map(result -> {
                    Long deptId = (Long) result[0];
                    Long count = (Long) result[1];

                    // Try to enrich with department name
                    String deptName = null;
                    if (deptId != null) {
                        try {
                            DepartmentDTO dept = departmentClient.getDepartment(deptId);
                            deptName = dept.getName();
                        } catch (Exception e) {
                            log.warn("Failed to fetch department name for id: {}", deptId);
                        }
                    }

                    return EmployeeStatsDTO.DepartmentCount.builder()
                            .departmentId(deptId)
                            .count(count)
                            .departmentName(deptName)
                            .build();
                })
                .collect(Collectors.toList());

        return EmployeeStatsDTO.builder()
                .totalEmployees(totalEmployees)
                .countsByDepartment(counts)
                .build();
    }

    private EmployeeDTO toDTO(Employee e) {
        DepartmentDTO dept = null;
        if (e.getDepartmentId() != null) {
            try {
                dept = departmentClient.getDepartment(e.getDepartmentId());
            } catch (Exception ignored) { }
        }
        return EmployeeDTO.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .departmentId(e.getDepartmentId())
                .department(dept)
                .build();
    }

    public List<EmployeeDTO> getByDepartmentId(Long departmentId) {
        List<Employee> employees = repository.findByDepartmentId(departmentId);
        return employees.stream()
                .map(this::toDTOWithoutDepartment)
                .toList();
    }

    public Long getCountByDepartmentId(Long departmentId) {
        return repository.countByDepartmentId(departmentId);
    }

    private EmployeeDTO toDTOWithoutDepartment(Employee e) {
        return EmployeeDTO.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .departmentId(e.getDepartmentId())
                .build();
    }
}
