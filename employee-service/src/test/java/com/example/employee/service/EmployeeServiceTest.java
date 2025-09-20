package com.example.employee.service;

import com.example.employee.client.DepartmentClient;
import com.example.employee.domain.Employee;
import com.example.employee.dto.DepartmentDTO;
import com.example.employee.dto.EmployeeDTO;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repo.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class EmployeeServiceTest {

    @Mock
    EmployeeRepository repository;

    @Mock
    DepartmentClient departmentClient;

    @InjectMocks
    EmployeeService service;

    @Nested
    @DisplayName("getAll() tests")
    class GetAllTests {

        @Test
        @DisplayName("returns empty list when no employees exist")
        void returns_empty_list_when_no_employees() {
            when(repository.findAll()).thenReturn(List.of());

            var result = service.getAll();

            assertThat(result).isEmpty();
            verify(repository).findAll();
        }

        @Test
        @DisplayName("returns all employees with department enrichment")
        void returns_all_employees_with_department_enrichment() {
            var employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(10L)
                .build();

            var department = DepartmentDTO.builder()
                .id(10L)
                .name("Engineering")
                .build();

            when(repository.findAll()).thenReturn(List.of(employee));
            when(departmentClient.getDepartment(10L)).thenReturn(department);

            var result = service.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getDepartment()).isNotNull();
            assertThat(result.get(0).getDepartment().getName()).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("handles department client failures gracefully")
        void handles_department_client_failures_gracefully() {
            var employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(10L)
                .build();

            when(repository.findAll()).thenReturn(List.of(employee));
            when(departmentClient.getDepartment(10L)).thenThrow(new RuntimeException("Service unavailable"));

            var result = service.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment()).isNull(); // Should gracefully handle failure
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @ParameterizedTest(name = "throws EmployeeNotFoundException for id {0}")
        @ValueSource(longs = {999L, 0L, -1L})
        @DisplayName("throws exception when employee not found")
        void throws_exception_when_employee_not_found(Long id) {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("Employee not found with id: " + id);
        }

        @Test
        @DisplayName("returns employee with department when found")
        void returns_employee_with_department_when_found() {
            var employee = Employee.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .departmentId(5L)
                .build();

            var department = DepartmentDTO.builder()
                .id(5L)
                .name("Marketing")
                .build();

            when(repository.findById(1L)).thenReturn(Optional.of(employee));
            when(departmentClient.getDepartment(5L)).thenReturn(department);

            var result = service.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getDepartment().getName()).isEqualTo("Marketing");
        }

        @Test
        @DisplayName("returns employee without department when departmentId is null")
        void returns_employee_without_department_when_departmentId_is_null() {
            var employee = Employee.builder()
                .id(1L)
                .firstName("Bob")
                .lastName("Wilson")
                .email("bob@example.com")
                .departmentId(null)
                .build();

            when(repository.findById(1L)).thenReturn(Optional.of(employee));

            var result = service.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDepartment()).isNull();
            verify(departmentClient, never()).getDepartment(anyLong());
        }
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @ParameterizedTest(name = "create({0}) → duplicate? {1}")
        @CsvSource({
                "dina@example.com, false",
                "alice@example.com, true",
                "bob@company.org, false",
                "existing@test.com, true"
        })
        @DisplayName("handles duplicate email validation")
        void handles_duplicate_email_validation(String email, boolean duplicate) {
            when(repository.existsByEmail(email)).thenReturn(duplicate);

            var dto = EmployeeDTO.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .departmentId(1L)
                .build();

            if (duplicate) {
                assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("Employee with email already exists: " + email);
                verify(repository, never()).save(any());
            } else {
                var savedEmployee = Employee.builder()
                    .id(101L)
                    .firstName("Test")
                    .lastName("User")
                    .email(email)
                    .departmentId(1L)
                    .build();

                when(repository.save(any(Employee.class))).thenReturn(savedEmployee);

                var result = service.create(dto);

                assertThat(result.getId()).isEqualTo(101L);
                assertThat(result.getEmail()).isEqualTo(email);
                verify(repository).save(any(Employee.class));
            }
        }

        @Test
        @DisplayName("creates employee with all fields correctly mapped")
        void creates_employee_with_all_fields_correctly_mapped() {
            var dto = EmployeeDTO.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .departmentId(2L)
                .build();

            when(repository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
                Employee employee = invocation.getArgument(0);
                employee.setId(123L);
                return employee;
            });

            var result = service.create(dto);

            assertThat(result.getId()).isEqualTo(123L);
            assertThat(result.getFirstName()).isEqualTo("Alice");
            assertThat(result.getLastName()).isEqualTo("Johnson");
            assertThat(result.getEmail()).isEqualTo("alice.johnson@example.com");
            assertThat(result.getDepartmentId()).isEqualTo(2L);

            verify(repository).existsByEmail("alice.johnson@example.com");
            verify(repository).save(argThat(emp ->
                emp.getFirstName().equals("Alice") &&
                emp.getLastName().equals("Johnson") &&
                emp.getEmail().equals("alice.johnson@example.com") &&
                emp.getDepartmentId().equals(2L)
            ));
        }

        @Test
        @DisplayName("creates employee without department when departmentId is null")
        void creates_employee_without_department_when_departmentId_is_null() {
            var dto = EmployeeDTO.builder()
                .firstName("Solo")
                .lastName("Worker")
                .email("solo@example.com")
                .departmentId(null)
                .build();

            when(repository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
                Employee employee = invocation.getArgument(0);
                employee.setId(456L);
                return employee;
            });

            var result = service.create(dto);

            assertThat(result.getId()).isEqualTo(456L);
            assertThat(result.getDepartmentId()).isNull();
            assertThat(result.getDepartment()).isNull();
        }
    }
}
