package com.example.department.web;

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
import com.example.department.exception.GlobalExceptionHandler;
import com.example.department.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("DepartmentController (WebMvc slice)")
public class DepartmentControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockBean
    DepartmentService service;

    @Nested
    @DisplayName("GET /departments (paginated)")
    class GetAllDepartmentsPaginated {

        @Test
        @DisplayName("returns paginated departments without filters")
        void returns_paginated_departments_without_filters() throws Exception {
            var pageResponse = PageResponse.<DepartmentDTO>builder()
                .content(List.of(
                    createDepartmentDTO(1L, "Engineering", "ENG"),
                    createDepartmentDTO(2L, "Marketing", "MKT")
                ))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

            when(service.getAllPaginated(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

            mvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Engineering"))
                .andExpect(jsonPath("$.content[1].name").value("Marketing"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("returns filtered departments by name and code")
        void returns_filtered_departments_by_name_and_code() throws Exception {
            var pageResponse = PageResponse.<DepartmentDTO>builder()
                .content(List.of(createDepartmentDTO(1L, "Engineering", "ENG")))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

            when(service.getAllPaginated(eq("Eng"), eq("ENG"), any(Pageable.class))).thenReturn(pageResponse);

            mvc.perform(get("/api/v1/departments")
                    .param("name", "Eng")
                    .param("code", "ENG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Engineering"));
        }

        @Test
        @DisplayName("accepts pagination parameters")
        void accepts_pagination_parameters() throws Exception {
            var pageResponse = PageResponse.<DepartmentDTO>builder()
                .content(List.of())
                .page(1)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(false)
                .last(true)
                .build();

            when(service.getAllPaginated(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

            mvc.perform(get("/api/v1/departments")
                    .param("page", "1")
                    .param("size", "10")
                    .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
        }
    }

    @Nested
    @DisplayName("GET /departments/search")
    class SearchDepartments {

        @Test
        @DisplayName("searches departments by term")
        void searches_departments_by_term() throws Exception {
            var pageResponse = PageResponse.<DepartmentDTO>builder()
                .content(List.of(createDepartmentDTO(1L, "Engineering", "ENG")))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

            when(service.searchDepartments(eq("Engineering"), any(Pageable.class))).thenReturn(pageResponse);

            mvc.perform(get("/api/v1/departments/search")
                    .param("q", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Engineering"));
        }

        @Test
        @DisplayName("requires search term parameter")
        void requires_search_term_parameter() throws Exception {
            mvc.perform(get("/api/v1/departments/search"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /departments/{id}")
    class GetDepartmentById {

        @Test
        @DisplayName("returns department when found")
        void returns_department_when_found() throws Exception {
            var department = createDepartmentDTO(1L, "Engineering", "ENG");
            when(service.getByIdAsDTO(1L)).thenReturn(department);

            mvc.perform(get("/api/v1/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.code").value("ENG"));
        }

        @Test
        @DisplayName("returns 404 when department not found")
        void returns_404_when_department_not_found() throws Exception {
            when(service.getByIdAsDTO(999L)).thenThrow(new DepartmentNotFoundException(999L));

            mvc.perform(get("/api/v1/departments/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /departments/by-code/{code}")
    class GetDepartmentByCode {

        @Test
        @DisplayName("returns department when found by code")
        void returns_department_when_found_by_code() throws Exception {
            var department = createDepartmentDTO(1L, "Engineering", "ENG");
            when(service.getByCode("ENG")).thenReturn(department);

            mvc.perform(get("/api/v1/departments/by-code/ENG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.code").value("ENG"));
        }

        @Test
        @DisplayName("returns 404 when department not found by code")
        void returns_404_when_department_not_found_by_code() throws Exception {
            when(service.getByCode("INVALID")).thenThrow(new DepartmentNotFoundException("Department not found with code: INVALID"));

            mvc.perform(get("/api/v1/departments/by-code/INVALID"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /departments/{id}/employees")
    class GetDepartmentEmployees {

        @Test
        @DisplayName("returns employees for department")
        void returns_employees_for_department() throws Exception {
            var employees = List.of(
                EmployeeDTO.builder().id(1L).firstName("John").lastName("Doe").email("john@example.com").departmentId(1L).build(),
                EmployeeDTO.builder().id(2L).firstName("Jane").lastName("Smith").email("jane@example.com").departmentId(1L).build()
            );

            when(service.getEmployeesByDepartmentId(1L)).thenReturn(employees);

            mvc.perform(get("/api/v1/departments/1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"));
        }

        @Test
        @DisplayName("returns empty array when no employees")
        void returns_empty_array_when_no_employees() throws Exception {
            when(service.getEmployeesByDepartmentId(1L)).thenReturn(List.of());

            mvc.perform(get("/api/v1/departments/1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /departments")
    class CreateDepartment {

        @Test
        @DisplayName("creates department successfully")
        void creates_department_successfully() throws Exception {
            var request = DepartmentDTO.builder()
                .name("Engineering")
                .code("ENG")
                .description("Software development")
                .managerEmail("manager@example.com")
                .location("Building A")
                .build();

            var response = createDepartmentDTO(1L, "Engineering", "ENG");
            when(service.createFromDTO(any(DepartmentDTO.class))).thenReturn(response);

            mvc.perform(post("/api/v1/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.code").value("ENG"));
        }

        @Test
        @DisplayName("validates required fields")
        void validates_required_fields() throws Exception {
            var request = DepartmentDTO.builder().build(); // Missing required fields

            mvc.perform(post("/api/v1/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when duplicate name")
        void returns_409_when_duplicate_name() throws Exception {
            var request = DepartmentDTO.builder()
                .name("Engineering")
                .code("ENG")
                .build();

            when(service.createFromDTO(any(DepartmentDTO.class)))
                .thenThrow(new DuplicateDepartmentException("Engineering"));

            mvc.perform(post("/api/v1/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when duplicate code")
        void returns_409_when_duplicate_code() throws Exception {
            var request = DepartmentDTO.builder()
                .name("Engineering")
                .code("ENG")
                .build();

            when(service.createFromDTO(any(DepartmentDTO.class)))
                .thenThrow(new DuplicateCodeException("ENG"));

            mvc.perform(post("/api/v1/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /departments/{id}")
    class UpdateDepartment {

        @Test
        @DisplayName("updates department successfully")
        void updates_department_successfully() throws Exception {
            var request = DepartmentUpdateRequest.builder()
                .name("Software Engineering")
                .code("SW-ENG")
                .description("Updated description")
                .managerEmail("new-manager@example.com")
                .location("Building B")
                .build();

            var response = createDepartmentDTO(1L, "Software Engineering", "SW-ENG");
            when(service.updateById(eq(1L), any(DepartmentUpdateRequest.class))).thenReturn(response);

            mvc.perform(put("/api/v1/departments/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Software Engineering"))
                .andExpect(jsonPath("$.code").value("SW-ENG"));
        }

        @Test
        @DisplayName("returns 404 when department not found")
        void returns_404_when_department_not_found() throws Exception {
            var request = DepartmentUpdateRequest.builder()
                .name("Engineering")
                .code("ENG")
                .build();

            when(service.updateById(eq(999L), any(DepartmentUpdateRequest.class)))
                .thenThrow(new DepartmentNotFoundException(999L));

            mvc.perform(put("/api/v1/departments/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /departments/{id}")
    class PatchDepartment {

        @Test
        @DisplayName("patches department fields")
        void patches_department_fields() throws Exception {
            var request = DepartmentPatchRequest.builder()
                .description("Updated description")
                .managerEmail("new-manager@example.com")
                .build();

            var response = createDepartmentDTO(1L, "Engineering", "ENG");
            response.setDescription("Updated description");
            response.setManagerEmail("new-manager@example.com");

            when(service.patchById(eq(1L), any(DepartmentPatchRequest.class))).thenReturn(response);

            mvc.perform(patch("/api/v1/departments/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Engineering")) // unchanged
                .andExpect(jsonPath("$.description").value("Updated description")) // changed
                .andExpect(jsonPath("$.managerEmail").value("new-manager@example.com")); // changed
        }

        @Test
        @DisplayName("handles empty patch request")
        void handles_empty_patch_request() throws Exception {
            var request = DepartmentPatchRequest.builder().build();
            var response = createDepartmentDTO(1L, "Engineering", "ENG");

            when(service.patchById(eq(1L), any(DepartmentPatchRequest.class))).thenReturn(response);

            mvc.perform(patch("/api/v1/departments/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(request)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /departments/{id}")
    class DeleteDepartment {

        @Test
        @DisplayName("deletes department successfully")
        void deletes_department_successfully() throws Exception {
            mvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isNoContent());

            verify(service).deleteById(1L);
        }

        @Test
        @DisplayName("returns 404 when department not found")
        void returns_404_when_department_not_found() throws Exception {
            doThrow(new DepartmentNotFoundException(999L)).when(service).deleteById(999L);

            mvc.perform(delete("/api/v1/departments/999"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when department has employees")
        void returns_400_when_department_has_employees() throws Exception {
            doThrow(new DepartmentInUseException(1L, 5L)).when(service).deleteById(1L);

            mvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /departments/legacy")
    class LegacyEndpoint {

        @Test
        @DisplayName("returns simple list of departments")
        void returns_simple_list_of_departments() throws Exception {
            when(service.getAll()).thenReturn(List.of(
                Department.builder().id(1L).name("Engineering").build(),
                Department.builder().id(2L).name("Marketing").build()
            ));

            mvc.perform(get("/api/v1/departments/legacy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Engineering"));
        }
    }

    private DepartmentDTO createDepartmentDTO(Long id, String name, String code) {
        return DepartmentDTO.builder()
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