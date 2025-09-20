package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.*;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Service Part 4 - Enhanced Functionality Tests")
class EmployeeServicePart4Test {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private DepartmentClient departmentClient;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee sampleEmployee;
    private DepartmentDTO sampleDepartment;

    @BeforeEach
    void setUp() {
        sampleEmployee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        sampleDepartment = DepartmentDTO.builder()
                .id(1L)
                .name("Engineering")
                .description("Software Development")
                .build();
    }

    @Test
    @DisplayName("getAllPaginated should return paginated results with filters")
    void testGetAllPaginated() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(sampleEmployee), pageable, 1);

        when(repository.findWithFilters(eq("john"), eq("doe"), eq(1L), any(Pageable.class)))
                .thenReturn(page);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment);

        // When
        PageResponse<EmployeeDTO> result = employeeService.getAllPaginated("john", "doe", 1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());

        EmployeeDTO employeeDto = result.getContent().get(0);
        assertEquals(sampleEmployee.getId(), employeeDto.getId());
        assertEquals(sampleEmployee.getFirstName(), employeeDto.getFirstName());
        assertNotNull(employeeDto.getDepartment());
        assertEquals(sampleDepartment.getName(), employeeDto.getDepartment().getName());
    }

    @Test
    @DisplayName("getAllPaginated without filters should use findAll")
    void testGetAllPaginatedWithoutFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(sampleEmployee), pageable, 1);

        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment);

        // When
        PageResponse<EmployeeDTO> result = employeeService.getAllPaginated(null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(repository).findAll(pageable);
        verify(repository, never()).findWithFilters(anyString(), anyString(), anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("update should update employee successfully")
    void testUpdate() {
        // Given
        Long employeeId = 1L;
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .departmentId(2L)
                .build();

        Employee existingEmployee = Employee.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        Employee updatedEmployee = Employee.builder()
                .id(employeeId)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .departmentId(2L)
                .build();

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.existsByEmailAndIdNot("jane.smith@example.com", employeeId)).thenReturn(false);
        when(repository.save(any(Employee.class))).thenReturn(updatedEmployee);

        // When
        EmployeeDTO result = employeeService.update(employeeId, request);

        // Then
        assertNotNull(result);
        assertEquals(employeeId, result.getId());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals("jane.smith@example.com", result.getEmail());
        assertEquals(2L, result.getDepartmentId());
    }

    @Test
    @DisplayName("update should throw DuplicateEmailException when email already exists")
    void testUpdateThrowsDuplicateEmailException() {
        // Given
        Long employeeId = 1L;
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("existing@example.com")
                .departmentId(2L)
                .build();

        Employee existingEmployee = Employee.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.existsByEmailAndIdNot("existing@example.com", employeeId)).thenReturn(true);

        // When & Then
        assertThrows(DuplicateEmailException.class, () ->
            employeeService.update(employeeId, request));

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("patch should partially update employee")
    void testPatch() {
        // Given
        Long employeeId = 1L;
        EmployeePatchRequest request = EmployeePatchRequest.builder()
                .firstName("Jane")
                .build();

        Employee existingEmployee = Employee.builder()
                .id(employeeId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        Employee updatedEmployee = Employee.builder()
                .id(employeeId)
                .firstName("Jane")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        when(repository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(repository.save(any(Employee.class))).thenReturn(updatedEmployee);

        // When
        EmployeeDTO result = employeeService.patch(employeeId, request);

        // Then
        assertNotNull(result);
        assertEquals(employeeId, result.getId());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Doe", result.getLastName()); // Unchanged
        assertEquals("john@example.com", result.getEmail()); // Unchanged
    }

    @Test
    @DisplayName("delete should delete employee successfully")
    void testDelete() {
        // Given
        Long employeeId = 1L;
        when(repository.existsById(employeeId)).thenReturn(true);

        // When
        employeeService.delete(employeeId);

        // Then
        verify(repository).deleteById(employeeId);
    }

    @Test
    @DisplayName("delete should throw EmployeeNotFoundException when employee doesn't exist")
    void testDeleteThrowsEmployeeNotFoundException() {
        // Given
        Long employeeId = 999L;
        when(repository.existsById(employeeId)).thenReturn(false);

        // When & Then
        assertThrows(EmployeeNotFoundException.class, () ->
            employeeService.delete(employeeId));

        verify(repository, never()).deleteById(employeeId);
    }

    @Test
    @DisplayName("search should return employees matching search term")
    void testSearch() {
        // Given
        String searchTerm = "john";
        List<Employee> employees = List.of(sampleEmployee);

        when(repository.searchByNameOrEmail(searchTerm)).thenReturn(employees);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment);

        // When
        List<EmployeeDTO> result = employeeService.search(searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleEmployee.getId(), result.get(0).getId());
        assertEquals(sampleEmployee.getFirstName(), result.get(0).getFirstName());
    }

    @Test
    @DisplayName("getStats should return employee statistics")
    void testGetStats() {
        // Given
        Object[] departmentCount = {1L, 5L}; // departmentId=1L, count=5L
        List<Object[]> departmentCounts = List.<Object[]>of(departmentCount);

        when(repository.count()).thenReturn(10L);
        when(repository.countByDepartmentIdGrouped()).thenReturn(departmentCounts);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment);

        // When
        EmployeeStatsDTO result = employeeService.getStats();

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotalEmployees());
        assertEquals(1, result.getCountsByDepartment().size());

        EmployeeStatsDTO.DepartmentCount deptCount = result.getCountsByDepartment().get(0);
        assertEquals(1L, deptCount.getDepartmentId());
        assertEquals(5L, deptCount.getCount());
        assertEquals("Engineering", deptCount.getDepartmentName());
    }

    @Test
    @DisplayName("create with idempotency key should cache and return same result")
    void testCreateWithIdempotencyKey() {
        // Given
        String idempotencyKey = "unique-key-123";
        EmployeeDTO requestDto = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(1L)
                .build();

        when(repository.existsByEmail("john@example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenReturn(sampleEmployee);
        when(departmentClient.getDepartment(1L)).thenReturn(sampleDepartment);

        // When - First call
        EmployeeDTO result1 = employeeService.create(requestDto, idempotencyKey);

        // When - Second call with same idempotency key
        EmployeeDTO result2 = employeeService.create(requestDto, idempotencyKey);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getId(), result2.getId());
        assertEquals(result1.getEmail(), result2.getEmail());

        // Repository save should only be called once
        verify(repository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("getById with enrichment flag should control department loading")
    void testGetByIdWithEnrichmentFlag() {
        // Given
        Long employeeId = 1L;
        when(repository.findById(employeeId)).thenReturn(Optional.of(sampleEmployee));

        // When - With enrichment
        EmployeeDTO resultWithDept = employeeService.getById(employeeId, true);

        // When - Without enrichment
        EmployeeDTO resultWithoutDept = employeeService.getById(employeeId, false);

        // Then
        assertNotNull(resultWithDept);
        assertNotNull(resultWithoutDept);

        // The enriched version should have department loaded (may be null if client fails)
        // The non-enriched version should not have department
        assertNull(resultWithoutDept.getDepartment());
    }
}