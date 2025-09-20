package com.example.department.service;

import com.example.department.domain.Department;
import com.example.department.dto.DepartmentDTO;
import com.example.department.dto.DepartmentPatchRequest;
import com.example.department.dto.DepartmentUpdateRequest;
import com.example.department.dto.EmployeeDTO;
import com.example.department.dto.PageResponse;
import com.example.department.exception.DepartmentInUseException;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateCodeException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.repo.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository repository;
    private final RestTemplate restTemplate;

    @Value("${employee.service.url:http://employee-service}")
    private String employeeServiceUrl;

    public List<Department> getAll() {
        return repository.findAll();
    }

    public PageResponse<DepartmentDTO> getAllPaginated(String name, String code, Pageable pageable) {
        Page<Department> page = repository.findAllWithFilters(name, code, pageable);

        List<DepartmentDTO> content = page.getContent().stream()
            .map(this::convertToDTO)
            .toList();

        return PageResponse.<DepartmentDTO>builder()
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

    public PageResponse<DepartmentDTO> searchDepartments(String searchTerm, Pageable pageable) {
        Page<Department> page = repository.searchByNameCodeOrDescription(searchTerm, pageable);

        List<DepartmentDTO> content = page.getContent().stream()
            .map(this::convertToDTO)
            .toList();

        return PageResponse.<DepartmentDTO>builder()
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

    public Department getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    public DepartmentDTO getByIdAsDTO(Long id) {
        return convertToDTO(getById(id));
    }

    public DepartmentDTO getByCode(String code) {
        Department department = repository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with code: " + code));
        return convertToDTO(department);
    }

    @Transactional
    public Department create(Department department) {
        validateUniqueConstraints(department.getName(), department.getCode(), null);
        return repository.save(department);
    }

    @Transactional
    public DepartmentDTO createFromDTO(DepartmentDTO departmentDTO) {
        validateUniqueConstraints(departmentDTO.getName(), departmentDTO.getCode(), null);

        Department department = Department.builder()
            .name(departmentDTO.getName())
            .code(departmentDTO.getCode())
            .description(departmentDTO.getDescription())
            .managerEmail(departmentDTO.getManagerEmail())
            .location(departmentDTO.getLocation())
            .build();

        Department saved = repository.save(department);
        return convertToDTO(saved);
    }

    @Transactional
    public DepartmentDTO updateById(Long id, DepartmentUpdateRequest updateRequest) {
        Department existing = getById(id);

        validateUniqueConstraints(updateRequest.getName(), updateRequest.getCode(), id);

        existing.setName(updateRequest.getName());
        existing.setCode(updateRequest.getCode());
        existing.setDescription(updateRequest.getDescription());
        existing.setManagerEmail(updateRequest.getManagerEmail());
        existing.setLocation(updateRequest.getLocation());

        Department updated = repository.save(existing);
        return convertToDTO(updated);
    }

    @Transactional
    public DepartmentDTO patchById(Long id, DepartmentPatchRequest patchRequest) {
        Department existing = getById(id);

        if (StringUtils.hasText(patchRequest.getName()) || StringUtils.hasText(patchRequest.getCode())) {
            String nameToCheck = StringUtils.hasText(patchRequest.getName()) ? patchRequest.getName() : existing.getName();
            String codeToCheck = StringUtils.hasText(patchRequest.getCode()) ? patchRequest.getCode() : existing.getCode();
            validateUniqueConstraints(nameToCheck, codeToCheck, id);
        }

        if (StringUtils.hasText(patchRequest.getName())) {
            existing.setName(patchRequest.getName());
        }
        if (StringUtils.hasText(patchRequest.getCode())) {
            existing.setCode(patchRequest.getCode());
        }
        if (patchRequest.getDescription() != null) {
            existing.setDescription(patchRequest.getDescription());
        }
        if (patchRequest.getManagerEmail() != null) {
            existing.setManagerEmail(patchRequest.getManagerEmail());
        }
        if (patchRequest.getLocation() != null) {
            existing.setLocation(patchRequest.getLocation());
        }

        Department updated = repository.save(existing);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteById(Long id) {
        Department department = getById(id);

        long employeeCount = getEmployeeCountForDepartment(id);
        if (employeeCount > 0) {
            throw new DepartmentInUseException(id, employeeCount);
        }

        repository.deleteById(id);
    }

    public List<EmployeeDTO> getEmployeesByDepartmentId(Long departmentId) {
        Department department = getById(departmentId);

        try {
            String url = employeeServiceUrl + "/employees/by-department/" + departmentId;
            EmployeeDTO[] employees = restTemplate.getForObject(url, EmployeeDTO[].class);
            return employees != null ? Arrays.asList(employees) : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch employees for department {}: {}", departmentId, e.getMessage());
            return List.of();
        }
    }

    private long getEmployeeCountForDepartment(Long departmentId) {
        try {
            String url = employeeServiceUrl + "/employees/count-by-department/" + departmentId;
            Long count = restTemplate.getForObject(url, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Failed to check employee count for department {}: {}", departmentId, e.getMessage());
            return 0;
        }
    }

    private void validateUniqueConstraints(String name, String code, Long excludeId) {
        if (excludeId == null) {
            if (repository.existsByNameIgnoreCase(name)) {
                throw new DuplicateDepartmentException(name);
            }
            if (repository.existsByCodeIgnoreCase(code)) {
                throw new DuplicateCodeException(code);
            }
        } else {
            if (repository.existsByNameIgnoreCaseAndIdNot(name, excludeId)) {
                throw new DuplicateDepartmentException(name);
            }
            if (repository.existsByCodeIgnoreCaseAndIdNot(code, excludeId)) {
                throw new DuplicateCodeException(code);
            }
        }
    }

    private DepartmentDTO convertToDTO(Department department) {
        return DepartmentDTO.builder()
            .id(department.getId())
            .name(department.getName())
            .code(department.getCode())
            .description(department.getDescription())
            .managerEmail(department.getManagerEmail())
            .location(department.getLocation())
            .createdAt(department.getCreatedAt())
            .updatedAt(department.getUpdatedAt())
            .build();
    }

    public Pageable createPageable(int page, int size, String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return PageRequest.of(page, size, Sort.by("id").ascending());
        }

        String[] sortParts = sort.split(",");
        String property = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}