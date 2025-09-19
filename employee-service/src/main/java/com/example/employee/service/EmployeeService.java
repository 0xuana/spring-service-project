package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentClient departmentClient;

    public List<EmployeeDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EmployeeDTO getById(Long id) {
        Employee e = repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        return toDTO(e);
    }

    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
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
        return toDTO(e);
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
}
