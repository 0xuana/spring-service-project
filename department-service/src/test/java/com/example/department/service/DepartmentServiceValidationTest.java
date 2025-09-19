package com.example.department.service;

import com.example.department.domain.Department;
import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.repo.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceValidationTest {

    @Mock
    private DepartmentRepository repository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void getById_DepartmentNotFound_ThrowsDepartmentNotFoundException() {
        // Given
        Long nonExistentId = 999L;
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        DepartmentNotFoundException exception = assertThrows(
            DepartmentNotFoundException.class,
            () -> departmentService.getById(nonExistentId)
        );

        assertEquals("Department not found with id: 999", exception.getMessage());
        verify(repository).findById(nonExistentId);
    }

    @Test
    void create_DuplicateName_ThrowsDuplicateDepartmentException() {
        // Given
        String duplicateName = "Engineering";
        when(repository.existsByNameIgnoreCase(duplicateName)).thenReturn(true);

        var department = Department.builder()
            .name(duplicateName)
            .description("Software development department")
            .build();

        // When & Then
        DuplicateDepartmentException exception = assertThrows(
            DuplicateDepartmentException.class,
            () -> departmentService.create(department)
        );

        assertEquals("Department with name already exists: " + duplicateName, exception.getMessage());
        verify(repository).existsByNameIgnoreCase(duplicateName);
        verify(repository, never()).save(any()); // Service should short-circuit - never attempt to save
    }

    @Test
    void create_UniqueName_SuccessfullyCreatesDepartment() {
        // Given
        String uniqueName = "Marketing";
        when(repository.existsByNameIgnoreCase(uniqueName)).thenReturn(false);

        var department = Department.builder()
            .name(uniqueName)
            .description("Marketing and communications department")
            .build();

        var savedDepartment = Department.builder()
            .id(1L)
            .name(uniqueName)
            .description("Marketing and communications department")
            .build();

        when(repository.save(any(Department.class))).thenReturn(savedDepartment);

        // When
        var result = departmentService.create(department);

        // Then
        assertNotNull(result);
        assertEquals(uniqueName, result.getName());
        assertEquals(1L, result.getId());
        verify(repository).existsByNameIgnoreCase(uniqueName);
        verify(repository).save(department);
    }
}