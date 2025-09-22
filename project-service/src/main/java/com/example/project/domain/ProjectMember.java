package com.example.project.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "employee_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull(message = "Employee ID is required")
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @NotBlank(message = "Role is required")
    @Size(min = 2, max = 60, message = "Role must be between 2 and 60 characters")
    @Column(nullable = false, length = 60)
    private String role;

    @NotNull(message = "Allocation percentage is required")
    @Min(value = 0, message = "Allocation percentage must be between 0 and 100")
    @Max(value = 100, message = "Allocation percentage must be between 0 and 100")
    @Column(name = "allocation_percent", nullable = false)
    private Integer allocationPercent;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;
}