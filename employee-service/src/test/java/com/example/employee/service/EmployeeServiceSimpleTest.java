package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceSimpleTest {

    @Mock
    EmployeeRepository repository;

    @Mock
    DepartmentClient departmentClient;

    @InjectMocks
    EmployeeService service;

    @Test
    void getAll_returnsEmptyList_whenNoEmployees() {
        when(repository.findAll()).thenReturn(List.of());

        var result = service.getAll();

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }

    @Test
    void getById_throwsException_whenEmployeeNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.getById(999L));
        verify(repository).findById(999L);
    }

    @Test
    void create_throwsException_whenEmailExists() {
        String duplicateEmail = "test@example.com";
        when(repository.existsByEmail(duplicateEmail)).thenReturn(true);

        var dto = EmployeeDTO.builder()
            .firstName("Test")
            .lastName("User")
            .email(duplicateEmail)
            .build();

        assertThrows(DuplicateEmailException.class, () -> service.create(dto));
        verify(repository).existsByEmail(duplicateEmail);
        verify(repository, never()).save(any());
    }

    @Test
    void create_success_whenValidData() {
        String email = "new@example.com";
        when(repository.existsByEmail(email)).thenReturn(false);

        var savedEmployee = Employee.builder()
            .id(1L)
            .firstName("Test")
            .lastName("User")
            .email(email)
            .build();

        when(repository.save(any(Employee.class))).thenReturn(savedEmployee);

        var dto = EmployeeDTO.builder()
            .firstName("Test")
            .lastName("User")
            .email(email)
            .build();

        var result = service.create(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(email, result.getEmail());
        verify(repository).existsByEmail(email);
        verify(repository).save(any(Employee.class));
    }
}