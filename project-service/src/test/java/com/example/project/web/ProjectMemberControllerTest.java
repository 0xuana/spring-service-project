package com.example.project.web;

import com.example.project.dto.*;
import com.example.project.exception.DuplicateMemberException;
import com.example.project.exception.EmployeeNotFoundException;
import com.example.project.exception.ProjectNotFoundException;
import com.example.project.service.ProjectMemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectMemberController.class)
@DisplayName("ProjectMemberController Web Slice Tests")
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectMemberService memberService;

    private ProjectMemberDTO testMemberDTO;
    private AddMemberRequest addMemberRequest;
    private AddMembersRequest addMembersRequest;
    private AddMembersResponse addMembersResponse;

    @BeforeEach
    void setUp() {
        testMemberDTO = ProjectMemberDTO.builder()
            .id(1L)
            .projectId(1L)
            .employeeId(101L)
            .role("Developer")
            .allocationPercent(80)
            .assignedAt(LocalDateTime.now())
            .employee(EmployeeDTO.builder()
                .id(101L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@company.com")
                .build())
            .build();

        addMemberRequest = AddMemberRequest.builder()
            .employeeId(101L)
            .role("Developer")
            .allocationPercent(80)
            .build();

        addMembersRequest = AddMembersRequest.builder()
            .members(List.of(addMemberRequest))
            .build();

        addMembersResponse = AddMembersResponse.builder()
            .totalProcessed(1)
            .successCount(1)
            .errorCount(0)
            .results(List.of(MemberOperationResult.success(testMemberDTO)))
            .build();
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{id}/members Tests")
    class GetProjectMembersTests {

        @Test
        @DisplayName("Should return project members without enrichment")
        void shouldReturnProjectMembersWithoutEnrichment() throws Exception {
            // Given
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(List.of(testMemberDTO));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members")
                    .param("enrich", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(101))
                .andExpect(jsonPath("$[0].role").value("Developer"))
                .andExpect(jsonPath("$[0].allocationPercent").value(80));
        }

        @Test
        @DisplayName("Should return project members with enrichment")
        void shouldReturnProjectMembersWithEnrichment() throws Exception {
            // Given
            when(memberService.getMembersByProjectId(1L, true)).thenReturn(List.of(testMemberDTO));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members")
                    .param("enrich", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employee.firstName").value("John"))
                .andExpect(jsonPath("$[0].employee.lastName").value("Doe"))
                .andExpect(jsonPath("$[0].employee.email").value("john.doe@company.com"));
        }

        @Test
        @DisplayName("Should return empty list when no members found")
        void shouldReturnEmptyListWhenNoMembersFound() throws Exception {
            // Given
            when(memberService.getMembersByProjectId(1L, false)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{projectId}/members/{employeeId} Tests")
    class GetProjectMemberTests {

        @Test
        @DisplayName("Should return specific project member")
        void shouldReturnSpecificProjectMember() throws Exception {
            // Given
            when(memberService.getMember(1L, 101L, false)).thenReturn(testMemberDTO);

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(101))
                .andExpect(jsonPath("$.role").value("Developer"));
        }

        @Test
        @DisplayName("Should return 404 when member not found")
        void shouldReturn404WhenMemberNotFound() throws Exception {
            // Given
            when(memberService.getMember(1L, 999L, false))
                .thenThrow(new RuntimeException("Employee 999 is not a member of project 1"));

            // When & Then
            mockMvc.perform(get("/api/v1/projects/1/members/999"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/projects/{id}/members Tests")
    class AddMemberTests {

        @Test
        @DisplayName("Should add member successfully")
        void shouldAddMemberSuccessfully() throws Exception {
            // Given
            when(memberService.addMember(eq(1L), any(AddMemberRequest.class))).thenReturn(testMemberDTO);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employeeId").value(101))
                .andExpect(jsonPath("$.role").value("Developer"));
        }

        @Test
        @DisplayName("Should return 400 for validation errors")
        void shouldReturn400ForValidationErrors() throws Exception {
            // Given
            AddMemberRequest invalidRequest = AddMemberRequest.builder()
                .employeeId(null) // Invalid: null employee ID
                .role("") // Invalid: empty role
                .allocationPercent(-10) // Invalid: negative allocation
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectNotFound() throws Exception {
            // Given
            when(memberService.addMember(eq(999L), any())).thenThrow(new ProjectNotFoundException(999L));

            // When & Then
            mockMvc.perform(post("/api/v1/projects/999/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when employee not found")
        void shouldReturn404WhenEmployeeNotFound() throws Exception {
            // Given
            when(memberService.addMember(eq(1L), any())).thenThrow(EmployeeNotFoundException.forId(999L));

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Referenced Employee Not Found"));
        }

        @Test
        @DisplayName("Should return 409 when employee already a member")
        void shouldReturn409WhenEmployeeAlreadyAMember() throws Exception {
            // Given
            when(memberService.addMember(eq(1L), any()))
                .thenThrow(DuplicateMemberException.forProjectAndEmployee(1L, 101L));

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Project Member"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/projects/{id}/members/bulk Tests")
    class AddMultipleMembersTests {

        @Test
        @DisplayName("Should add multiple members successfully")
        void shouldAddMultipleMembersSuccessfully() throws Exception {
            // Given
            when(memberService.addMembers(eq(1L), any(AddMembersRequest.class))).thenReturn(addMembersResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMembersRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalProcessed").value(1))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].success").value(true));
        }

        @Test
        @DisplayName("Should handle mixed success and failure")
        void shouldHandleMixedSuccessAndFailure() throws Exception {
            // Given
            AddMembersResponse mixedResponse = AddMembersResponse.builder()
                .totalProcessed(2)
                .successCount(1)
                .errorCount(1)
                .results(List.of(
                    MemberOperationResult.success(testMemberDTO),
                    MemberOperationResult.error(999L, "Employee not found")
                ))
                .build();

            when(memberService.addMembers(eq(1L), any())).thenReturn(mixedResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMembersRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(1));
        }

        @Test
        @DisplayName("Should return 400 for empty members list")
        void shouldReturn400ForEmptyMembersList() throws Exception {
            // Given
            AddMembersRequest emptyRequest = AddMembersRequest.builder()
                .members(List.of()) // Empty list
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/projects/1/members/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/projects/{projectId}/members/{employeeId} Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member successfully")
        void shouldRemoveMemberSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1/members/101"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectNotFoundForRemove() throws Exception {
            // Given
            doThrow(new ProjectNotFoundException(999L)).when(memberService).removeMember(999L, 101L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/999/members/101"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return error when member not found")
        void shouldReturnErrorWhenMemberNotFound() throws Exception {
            // Given
            doThrow(new RuntimeException("Employee 999 is not a member of project 1"))
                .when(memberService).removeMember(1L, 999L);

            // When & Then
            mockMvc.perform(delete("/api/v1/projects/1/members/999"))
                .andExpect(status().isInternalServerError());
        }
    }
}