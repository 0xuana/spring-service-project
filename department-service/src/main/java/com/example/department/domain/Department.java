package com.example.department.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments", schema = "department",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_department_code", columnNames = "code"),
           @UniqueConstraint(name = "uk_department_name", columnNames = "name")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "code is required")
    @Size(min = 2, max = 20, message = "code must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "code must contain only uppercase letters, digits, and hyphens")
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    @Column(columnDefinition = "text")
    private String description;

    @Email(message = "managerEmail must be a valid email address")
    @Size(max = 200, message = "managerEmail must not exceed 200 characters")
    @Column(length = 200)
    private String managerEmail;

    @Size(max = 100, message = "location must not exceed 100 characters")
    @Column(length = 100)
    private String location;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
