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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DepartmentServiceTest {

    @Mock
    DepartmentRepository repository;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    DepartmentService service;

    @Nested
    @DisplayName("getAll() tests")
    class GetAllTests {

        @Test
        @DisplayName("returns empty list when no departments exist")
        void returns_empty_list_when_no_departments() {
            when(repository.findAll()).thenReturn(List.of());

            var result = service.getAll();

            assertThat(result).isEmpty();
            verify(repository).findAll();
        }

        @Test
        @DisplayName("returns all departments")
        void returns_all_departments() {
            var departments = List.of(
                Department.builder()
                    .id(1L)
                    .name("Engineering")
                    .description("Software development")
                    .build(),
                Department.builder()
                    .id(2L)
                    .name("Marketing")
                    .description("Product marketing")
                    .build()
            );

            when(repository.findAll()).thenReturn(departments);

            var result = service.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Engineering");
            assertThat(result.get(1).getName()).isEqualTo("Marketing");
            verify(repository).findAll();
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @ParameterizedTest(name = "throws DepartmentNotFoundException for id {0}")
        @ValueSource(longs = {999L, 0L, -1L, 100L})
        @DisplayName("throws exception when department not found")
        void throws_exception_when_department_not_found(Long id) {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(DepartmentNotFoundException.class)
                .hasMessageContaining("Department not found with id: " + id);
        }

        @Test
        @DisplayName("returns department when found")
        void returns_department_when_found() {
            var department = Department.builder()
                .id(5L)
                .name("Human Resources")
                .description("People operations")
                .build();

            when(repository.findById(5L)).thenReturn(Optional.of(department));

            var result = service.getById(5L);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getName()).isEqualTo("Human Resources");
            assertThat(result.getDescription()).isEqualTo("People operations");
            verify(repository).findById(5L);
        }
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @ParameterizedTest(name = "create({0}) → duplicate? {1}")
        @CsvSource({
                "Engineering, false",
                "Marketing, true",
                "Sales, false",
                "ENGINEERING, true"  // case insensitive check
        })
        @DisplayName("handles duplicate name validation")
        void handles_duplicate_name_validation(String name, boolean duplicate) {
            when(repository.existsByNameIgnoreCase(name)).thenReturn(duplicate);

            var department = Department.builder()
                .name(name)
                .description("Test department")
                .build();

            if (duplicate) {
                assertThatThrownBy(() -> service.create(department))
                    .isInstanceOf(DuplicateDepartmentException.class)
                    .hasMessageContaining("Department with name already exists: " + name);
                verify(repository, never()).save(any());
            } else {
                var savedDepartment = Department.builder()
                    .id(101L)
                    .name(name)
                    .description("Test department")
                    .build();

                when(repository.save(any(Department.class))).thenReturn(savedDepartment);

                var result = service.create(department);

                assertThat(result.getId()).isEqualTo(101L);
                assertThat(result.getName()).isEqualTo(name);
                verify(repository).save(any(Department.class));
            }
        }

        @Test
        @DisplayName("creates department with all fields correctly mapped")
        void creates_department_with_all_fields_correctly_mapped() {
            var department = Department.builder()
                .name("Finance")
                .description("Financial operations and accounting")
                .build();

            when(repository.existsByNameIgnoreCase("Finance")).thenReturn(false);
            when(repository.save(any(Department.class))).thenAnswer(invocation -> {
                Department dept = invocation.getArgument(0);
                dept.setId(123L);
                return dept;
            });

            var result = service.create(department);

            assertThat(result.getId()).isEqualTo(123L);
            assertThat(result.getName()).isEqualTo("Finance");
            assertThat(result.getDescription()).isEqualTo("Financial operations and accounting");

            verify(repository).existsByNameIgnoreCase("Finance");
            verify(repository).save(argThat(dept ->
                dept.getName().equals("Finance") &&
                dept.getDescription().equals("Financial operations and accounting")
            ));
        }

        @Test
        @DisplayName("creates department with minimal data")
        void creates_department_with_minimal_data() {
            var department = Department.builder()
                .name("Legal")
                .description(null)  // description can be null
                .build();

            when(repository.existsByNameIgnoreCase("Legal")).thenReturn(false);
            when(repository.save(any(Department.class))).thenAnswer(invocation -> {
                Department dept = invocation.getArgument(0);
                dept.setId(456L);
                return dept;
            });

            var result = service.create(department);

            assertThat(result.getId()).isEqualTo(456L);
            assertThat(result.getName()).isEqualTo("Legal");
            assertThat(result.getDescription()).isNull();
        }

        @ParameterizedTest(name = "case insensitive duplicate check for: {0}")
        @CsvSource({
                "engineering, Engineering",
                "MARKETING, marketing",
                "SaLeS, sales",
                "Hr, hr"
        })
        @DisplayName("performs case insensitive duplicate check")
        void performs_case_insensitive_duplicate_check(String existingName, String newName) {
            when(repository.existsByNameIgnoreCase(newName)).thenReturn(true);

            var department = Department.builder()
                .name(newName)
                .description("Test")
                .build();

            assertThatThrownBy(() -> service.create(department))
                .isInstanceOf(DuplicateDepartmentException.class)
                .hasMessageContaining("Department with name already exists: " + newName);

            verify(repository).existsByNameIgnoreCase(newName);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllPaginated() tests")
    class GetAllPaginatedTests {

        @Test
        @DisplayName("returns paginated results without filters")
        void returns_paginated_results_without_filters() {
            var departments = List.of(
                createDepartment(1L, "Engineering", "ENG"),
                createDepartment(2L, "Marketing", "MKT")
            );
            var page = new PageImpl<>(departments, PageRequest.of(0, 20), 2);
            when(repository.findAll(any(Pageable.class))).thenReturn(page);

            var result = service.getAllPaginated(null, null, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);
            verify(repository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("returns filtered results with name and code")
        void returns_filtered_results_with_name_and_code() {
            var departments = List.of(createDepartment(1L, "Engineering", "ENG"));
            var page = new PageImpl<>(departments, PageRequest.of(0, 20), 1);
            when(repository.findWithFilters(eq("Eng"), eq("ENG"), any(Pageable.class))).thenReturn(page);

            var result = service.getAllPaginated("Eng", "ENG", PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Engineering");
            verify(repository).findWithFilters("Eng", "ENG", any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getByCode() tests")
    class GetByCodeTests {

        @Test
        @DisplayName("returns department when found by code")
        void returns_department_when_found_by_code() {
            var department = createDepartment(1L, "Engineering", "ENG");
            when(repository.findByCodeIgnoreCase("ENG")).thenReturn(Optional.of(department));

            var result = service.getByCode("ENG");

            assertThat(result.getName()).isEqualTo("Engineering");
            assertThat(result.getCode()).isEqualTo("ENG");
            verify(repository).findByCodeIgnoreCase("ENG");
        }

        @Test
        @DisplayName("throws exception when department not found by code")
        void throws_exception_when_department_not_found_by_code() {
            when(repository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByCode("INVALID"))
                .isInstanceOf(DepartmentNotFoundException.class)
                .hasMessageContaining("Department not found with code: INVALID");
        }
    }

    @Nested
    @DisplayName("createFromDTO() tests")
    class CreateFromDTOTests {

        @Test
        @DisplayName("creates department from DTO with code validation")
        void creates_department_from_dto_with_code_validation() {
            var dto = DepartmentDTO.builder()
                .name("Engineering")
                .code("ENG")
                .description("Software development")
                .managerEmail("manager@example.com")
                .location("Building A")
                .build();

            when(repository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
            when(repository.existsByCodeIgnoreCase("ENG")).thenReturn(false);
            when(repository.save(any(Department.class))).thenAnswer(invocation -> {
                Department dept = invocation.getArgument(0);
                dept.setId(1L);
                dept.setCreatedAt(LocalDateTime.now());
                dept.setUpdatedAt(LocalDateTime.now());
                return dept;
            });

            var result = service.createFromDTO(dto);

            assertThat(result.getName()).isEqualTo("Engineering");
            assertThat(result.getCode()).isEqualTo("ENG");
            assertThat(result.getManagerEmail()).isEqualTo("manager@example.com");
            verify(repository).existsByNameIgnoreCase("Engineering");
            verify(repository).existsByCodeIgnoreCase("ENG");
            verify(repository).save(any(Department.class));
        }

        @Test
        @DisplayName("throws exception when code already exists")
        void throws_exception_when_code_already_exists() {
            var dto = DepartmentDTO.builder()
                .name("Engineering")
                .code("ENG")
                .build();

            when(repository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
            when(repository.existsByCodeIgnoreCase("ENG")).thenReturn(true);

            assertThatThrownBy(() -> service.createFromDTO(dto))
                .isInstanceOf(DuplicateCodeException.class)
                .hasMessageContaining("Department with code already exists: ENG");
        }
    }

    @Nested
    @DisplayName("updateById() tests")
    class UpdateByIdTests {

        @Test
        @DisplayName("updates all fields successfully")
        void updates_all_fields_successfully() {
            var existing = createDepartment(1L, "Engineering", "ENG");
            var updateRequest = DepartmentUpdateRequest.builder()
                .name("Software Engineering")
                .code("SW-ENG")
                .description("Updated description")
                .managerEmail("new-manager@example.com")
                .location("Building B")
                .build();

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByNameIgnoreCaseAndIdNot("Software Engineering", 1L)).thenReturn(false);
            when(repository.existsByCodeIgnoreCaseAndIdNot("SW-ENG", 1L)).thenReturn(false);
            when(repository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.updateById(1L, updateRequest);

            assertThat(result.getName()).isEqualTo("Software Engineering");
            assertThat(result.getCode()).isEqualTo("SW-ENG");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            verify(repository).save(existing);
        }

        @Test
        @DisplayName("throws exception when updating to existing name")
        void throws_exception_when_updating_to_existing_name() {
            var existing = createDepartment(1L, "Engineering", "ENG");
            var updateRequest = DepartmentUpdateRequest.builder()
                .name("Marketing")
                .code("MKT")
                .build();

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByNameIgnoreCaseAndIdNot("Marketing", 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.updateById(1L, updateRequest))
                .isInstanceOf(DuplicateDepartmentException.class)
                .hasMessageContaining("Department with name already exists: Marketing");
        }
    }

    @Nested
    @DisplayName("patchById() tests")
    class PatchByIdTests {

        @Test
        @DisplayName("patches only provided fields")
        void patches_only_provided_fields() {
            var existing = createDepartment(1L, "Engineering", "ENG");
            existing.setDescription("Original description");
            var patchRequest = DepartmentPatchRequest.builder()
                .description("Updated description")
                .managerEmail("new-manager@example.com")
                .build();

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.patchById(1L, patchRequest);

            assertThat(result.getName()).isEqualTo("Engineering"); // unchanged
            assertThat(result.getCode()).isEqualTo("ENG"); // unchanged
            assertThat(result.getDescription()).isEqualTo("Updated description"); // changed
            assertThat(result.getManagerEmail()).isEqualTo("new-manager@example.com"); // changed
        }

        @Test
        @DisplayName("validates uniqueness when patching name or code")
        void validates_uniqueness_when_patching_name_or_code() {
            var existing = createDepartment(1L, "Engineering", "ENG");
            var patchRequest = DepartmentPatchRequest.builder()
                .name("Marketing")
                .build();

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByNameIgnoreCaseAndIdNot("Marketing", 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.patchById(1L, patchRequest))
                .isInstanceOf(DuplicateDepartmentException.class);
        }
    }

    @Nested
    @DisplayName("deleteById() tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("deletes department when no employees assigned")
        void deletes_department_when_no_employees_assigned() {
            var department = createDepartment(1L, "Engineering", "ENG");
            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(restTemplate.getForObject(anyString(), eq(Long.class))).thenReturn(0L);

            service.deleteById(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("throws exception when employees are assigned")
        void throws_exception_when_employees_are_assigned() {
            var department = createDepartment(1L, "Engineering", "ENG");
            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(restTemplate.getForObject(anyString(), eq(Long.class))).thenReturn(5L);

            assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(DepartmentInUseException.class)
                .hasMessageContaining("Cannot delete department 1: 5 employees are still assigned");

            verify(repository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("handles employee service communication failure gracefully")
        void handles_employee_service_communication_failure_gracefully() {
            var department = createDepartment(1L, "Engineering", "ENG");
            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(restTemplate.getForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("Service unavailable"));

            service.deleteById(1L);

            verify(repository).deleteById(1L); // Should proceed with deletion when can't check
        }
    }

    @Nested
    @DisplayName("getEmployeesByDepartmentId() tests")
    class GetEmployeesByDepartmentIdTests {

        @Test
        @DisplayName("returns employees for department")
        void returns_employees_for_department() {
            var department = createDepartment(1L, "Engineering", "ENG");
            var employees = new EmployeeDTO[]{
                EmployeeDTO.builder().id(1L).firstName("John").lastName("Doe").email("john@example.com").departmentId(1L).build(),
                EmployeeDTO.builder().id(2L).firstName("Jane").lastName("Smith").email("jane@example.com").departmentId(1L).build()
            };

            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(restTemplate.getForObject(anyString(), eq(EmployeeDTO[].class))).thenReturn(employees);

            var result = service.getEmployeesByDepartmentId(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getFirstName()).isEqualTo("John");
            assertThat(result.get(1).getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("returns empty list when employee service fails")
        void returns_empty_list_when_employee_service_fails() {
            var department = createDepartment(1L, "Engineering", "ENG");
            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(restTemplate.getForObject(anyString(), eq(EmployeeDTO[].class))).thenThrow(new RuntimeException("Service unavailable"));

            var result = service.getEmployeesByDepartmentId(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createPageable() tests")
    class CreatePageableTests {

        @Test
        @DisplayName("creates default pageable when sort is null")
        void creates_default_pageable_when_sort_is_null() {
            var result = service.createPageable(0, 20, null);

            assertThat(result.getPageNumber()).isEqualTo(0);
            assertThat(result.getPageSize()).isEqualTo(20);
            assertThat(result.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @ParameterizedTest
        @CsvSource({
            "name,asc, name, ASC",
            "code,desc, code, DESC",
            "id,asc, id, ASC"
        })
        @DisplayName("creates pageable with correct sort")
        void creates_pageable_with_correct_sort(String property, String direction, String expectedProperty, Sort.Direction expectedDirection) {
            var result = service.createPageable(1, 10, property + "," + direction);

            assertThat(result.getPageNumber()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getSort().getOrderFor(expectedProperty).getDirection()).isEqualTo(expectedDirection);
        }
    }

    private Department createDepartment(Long id, String name, String code) {
        return Department.builder()
            .id(id)
            .name(name)
            .code(code)
            .description("Test description")
            .managerEmail("manager@example.com")
            .location("Test location")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}