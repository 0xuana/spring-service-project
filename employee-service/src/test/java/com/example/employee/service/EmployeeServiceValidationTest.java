package com.example.employee.service;

import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
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
class EmployeeServiceValidationTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void getById_EmployeeNotFound_ThrowsEmployeeNotFoundException() {
        // Given
        Long nonExistentId = 999L;
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        EmployeeNotFoundException exception = assertThrows(
            EmployeeNotFoundException.class,
            () -> employeeService.getById(nonExistentId)
        );

        assertEquals("Employee not found with id: 999", exception.getMessage());
        verify(repository).findById(nonExistentId);
    }

    @Test
    void create_DuplicateEmail_ThrowsDuplicateEmailException() {
        // Given
        String duplicateEmail = "john.doe@example.com";
        when(repository.existsByEmail(duplicateEmail)).thenReturn(true);

        var employeeDTO = com.example.employee.dto.EmployeeDTO.builder()
            .firstName("John")
            .lastName("Doe")
            .email(duplicateEmail)
            .departmentId(1L)
            .build();

        // When & Then
        DuplicateEmailException exception = assertThrows(
            DuplicateEmailException.class,
            () -> employeeService.create(employeeDTO)
        );

        assertEquals("Employee with email already exists: " + duplicateEmail, exception.getMessage());
        verify(repository).existsByEmail(duplicateEmail);
        verify(repository, never()).save(any()); // Service should short-circuit - never attempt to save
    }

    @Test
    void create_UniqueEmail_SuccessfullyCreatesEmployee() {
        // Given
        String uniqueEmail = "jane.doe@example.com";
        when(repository.existsByEmail(uniqueEmail)).thenReturn(false);

        var employee = com.example.employee.domain.Employee.builder()
            .id(1L)
            .firstName("Jane")
            .lastName("Doe")
            .email(uniqueEmail)
            .departmentId(1L)
            .build();

        when(repository.save(any())).thenReturn(employee);

        var employeeDTO = com.example.employee.dto.EmployeeDTO.builder()
            .firstName("Jane")
            .lastName("Doe")
            .email(uniqueEmail)
            .departmentId(1L)
            .build();

        // When
        var result = employeeService.create(employeeDTO);

        // Then
        assertNotNull(result);
        assertEquals(uniqueEmail, result.getEmail());
        verify(repository).existsByEmail(uniqueEmail);
        verify(repository).save(any());
    }
}