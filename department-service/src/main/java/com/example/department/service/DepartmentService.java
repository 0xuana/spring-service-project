package com.example.department.service;

import com.example.department.domain.Department;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.repo.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository repository;

    public List<Department> getAll() {
        return repository.findAll();
    }

    public Department getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    @Transactional
    public Department create(Department department) {
        if (repository.existsByNameIgnoreCase(department.getName())) {
            throw new DuplicateDepartmentException(department.getName());
        }
        return repository.save(department);
    }
}