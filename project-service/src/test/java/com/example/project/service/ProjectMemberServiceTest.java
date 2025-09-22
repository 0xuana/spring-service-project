package com.example.project.service;

import com.example.project.client.EmployeeClient;
import com.example.project.domain.Project;
import com.example.project.domain.ProjectMember;
import com.example.project.domain.ProjectStatus;
import com.example.project.dto.*;
import com.example.project.exception.DuplicateMemberException;
import com.example.project.exception.EmployeeNotFoundException;
import com.example.project.exception.ProjectNotFoundException;
import com.example.project.repo.ProjectMemberRepository;
import com.example.project.repo.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMemberService Unit Tests")
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository memberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private ProjectMemberService memberService;

    private Project testProject;
    private ProjectMember testMember;
    private AddMemberRequest addMemberRequest;
    private EmployeeDTO employeeDTO;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
            .id(1L)
            .code("TEST-001")
            .name("Test Project")
            .status(ProjectStatus.ACTIVE)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .build();

        testMember = ProjectMember.builder()
            .id(1L)
            .project(testProject)
            .employeeId(101L)
            .role("Developer")
            .allocationPercent(80)
            .assignedAt(LocalDateTime.now())
            .build();

        addMemberRequest = AddMemberRequest.builder()
            .employeeId(101L)
            .role("Developer")
            .allocationPercent(80)
            .build();

        employeeDTO = EmployeeDTO.builder()
            .id(101L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@company.com")
            .build();
    }

    @Nested
    @DisplayName("Get Members By Project ID Tests")
    class GetMembersByProjectIdTests {

        @Test
        @DisplayName("Should return members without enrichment")
        void shouldReturnMembersWithoutEnrichment() {
            // Given
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of(testMember));

            // When
            List<ProjectMemberDTO> result = memberService.getMembersByProjectId(1L, false);

            // Then
            assertThat(result).hasSize(1);
            ProjectMemberDTO memberDTO = result.get(0);
            assertThat(memberDTO.getEmployeeId()).isEqualTo(101L);
            assertThat(memberDTO.getRole()).isEqualTo("Developer");
            assertThat(memberDTO.getAllocationPercent()).isEqualTo(80);
            assertThat(memberDTO.getEmployee()).isNull();
        }

        @Test
        @DisplayName("Should return members with enrichment")
        void shouldReturnMembersWithEnrichment() {
            // Given
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of(testMember));
            when(employeeClient.getEmployeesByIds(List.of(101L))).thenReturn(List.of(employeeDTO));

            // When
            List<ProjectMemberDTO> result = memberService.getMembersByProjectId(1L, true);

            // Then
            assertThat(result).hasSize(1);
            ProjectMemberDTO memberDTO = result.get(0);
            assertThat(memberDTO.getEmployee()).isNotNull();
            assertThat(memberDTO.getEmployee().getFirstName()).isEqualTo("John");
            assertThat(memberDTO.getEmployee().getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should return empty list when no members found")
        void shouldReturnEmptyListWhenNoMembersFound() {
            // Given
            when(memberRepository.findByProjectId(1L)).thenReturn(new ArrayList<>());

            // When
            List<ProjectMemberDTO> result = memberService.getMembersByProjectId(1L, false);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle enrichment failure gracefully")
        void shouldHandleEnrichmentFailureGracefully() {
            // Given
            when(memberRepository.findByProjectId(1L)).thenReturn(List.of(testMember));
            when(employeeClient.getEmployeesByIds(any())).thenThrow(new RuntimeException("Service unavailable"));

            // When
            List<ProjectMemberDTO> result = memberService.getMembersByProjectId(1L, true);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmployee()).isNull(); // Enrichment failed, but member still returned
        }
    }

    @Nested
    @DisplayName("Add Member Tests")
    class AddMemberTests {

        @Test
        @DisplayName("Should add member successfully")
        void shouldAddMemberSuccessfully() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(employeeDTO);
            when(memberRepository.existsByProjectIdAndEmployeeId(1L, 101L)).thenReturn(false);
            when(memberRepository.save(any(ProjectMember.class))).thenReturn(testMember);

            // When
            ProjectMemberDTO result = memberService.addMember(1L, addMemberRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmployeeId()).isEqualTo(101L);
            assertThat(result.getRole()).isEqualTo("Developer");
            verify(memberRepository).save(any(ProjectMember.class));
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void shouldThrowExceptionWhenProjectNotFound() {
            // Given
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> memberService.addMember(999L, addMemberRequest))
                .isInstanceOf(ProjectNotFoundException.class);

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when employee not found")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> memberService.addMember(1L, addMemberRequest))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("101");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when employee already a member")
        void shouldThrowExceptionWhenEmployeeAlreadyAMember() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(employeeDTO);
            when(memberRepository.existsByProjectIdAndEmployeeId(1L, 101L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> memberService.addMember(1L, addMemberRequest))
                .isInstanceOf(DuplicateMemberException.class)
                .hasMessageContaining("already a member");

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when employee service fails")
        void shouldThrowExceptionWhenEmployeeServiceFails() {
            // Given
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenThrow(new RuntimeException("Service error"));

            // When & Then
            assertThatThrownBy(() -> memberService.addMember(1L, addMemberRequest))
                .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Add Multiple Members Tests")
    class AddMultipleMembersTests {

        @Test
        @DisplayName("Should add multiple members successfully")
        void shouldAddMultipleMembersSuccessfully() {
            // Given
            AddMemberRequest member1 = AddMemberRequest.builder()
                .employeeId(101L).role("Developer").allocationPercent(80).build();
            AddMemberRequest member2 = AddMemberRequest.builder()
                .employeeId(102L).role("Tester").allocationPercent(60).build();
            AddMembersRequest request = AddMembersRequest.builder()
                .members(List.of(member1, member2)).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(employeeDTO);
            when(employeeClient.getEmployeeById(102L, false)).thenReturn(
                EmployeeDTO.builder().id(102L).firstName("Jane").lastName("Smith").email("jane@company.com").build());
            when(memberRepository.existsByProjectIdAndEmployeeId(eq(1L), anyLong())).thenReturn(false);
            when(memberRepository.save(any(ProjectMember.class))).thenReturn(testMember);

            // When
            AddMembersResponse result = memberService.addMembers(1L, request);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(2);
            assertThat(result.getSuccessCount()).isEqualTo(2);
            assertThat(result.getErrorCount()).isEqualTo(0);
            assertThat(result.getResults()).hasSize(2);
            verify(memberRepository, times(2)).save(any(ProjectMember.class));
        }

        @Test
        @DisplayName("Should handle mixed success and failure")
        void shouldHandleMixedSuccessAndFailure() {
            // Given
            AddMemberRequest validMember = AddMemberRequest.builder()
                .employeeId(101L).role("Developer").allocationPercent(80).build();
            AddMemberRequest invalidMember = AddMemberRequest.builder()
                .employeeId(999L).role("Tester").allocationPercent(60).build();
            AddMembersRequest request = AddMembersRequest.builder()
                .members(List.of(validMember, invalidMember)).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(employeeClient.getEmployeeById(101L, false)).thenReturn(employeeDTO);
            when(employeeClient.getEmployeeById(999L, false)).thenReturn(null);
            when(memberRepository.existsByProjectIdAndEmployeeId(1L, 101L)).thenReturn(false);
            when(memberRepository.save(any(ProjectMember.class))).thenReturn(testMember);

            // When
            AddMembersResponse result = memberService.addMembers(1L, request);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(2);
            assertThat(result.getSuccessCount()).isEqualTo(1);
            assertThat(result.getErrorCount()).isEqualTo(1);
            assertThat(result.getResults()).hasSize(2);

            // Check success result
            MemberOperationResult successResult = result.getResults().stream()
                .filter(MemberOperationResult::isSuccess)
                .findFirst().orElse(null);
            assertThat(successResult).isNotNull();
            assertThat(successResult.getEmployeeId()).isEqualTo(101L);

            // Check error result
            MemberOperationResult errorResult = result.getResults().stream()
                .filter(r -> !r.isSuccess())
                .findFirst().orElse(null);
            assertThat(errorResult).isNotNull();
            assertThat(errorResult.getEmployeeId()).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("Remove Member Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member successfully")
        void shouldRemoveMemberSuccessfully() {
            // Given
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(memberRepository.findByProjectIdAndEmployeeId(1L, 101L)).thenReturn(Optional.of(testMember));

            // When
            memberService.removeMember(1L, 101L);

            // Then
            verify(memberRepository).delete(testMember);
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void shouldThrowExceptionWhenProjectNotFoundForRemove() {
            // Given
            when(projectRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> memberService.removeMember(999L, 101L))
                .isInstanceOf(ProjectNotFoundException.class);

            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when member not found")
        void shouldThrowExceptionWhenMemberNotFound() {
            // Given
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(memberRepository.findByProjectIdAndEmployeeId(1L, 999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> memberService.removeMember(1L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not a member");

            verify(memberRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Get Member Tests")
    class GetMemberTests {

        @Test
        @DisplayName("Should return member without enrichment")
        void shouldReturnMemberWithoutEnrichment() {
            // Given
            when(memberRepository.findByProjectIdAndEmployeeId(1L, 101L)).thenReturn(Optional.of(testMember));

            // When
            ProjectMemberDTO result = memberService.getMember(1L, 101L, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmployeeId()).isEqualTo(101L);
            assertThat(result.getEmployee()).isNull();
        }

        @Test
        @DisplayName("Should return member with enrichment")
        void shouldReturnMemberWithEnrichment() {
            // Given
            when(memberRepository.findByProjectIdAndEmployeeId(1L, 101L)).thenReturn(Optional.of(testMember));
            when(employeeClient.getEmployeesByIds(List.of(101L))).thenReturn(List.of(employeeDTO));

            // When
            ProjectMemberDTO result = memberService.getMember(1L, 101L, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmployee()).isNotNull();
            assertThat(result.getEmployee().getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should throw exception when member not found")
        void shouldThrowExceptionWhenMemberNotFoundForGet() {
            // Given
            when(memberRepository.findByProjectIdAndEmployeeId(1L, 999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> memberService.getMember(1L, 999L, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not a member");
        }
    }
}