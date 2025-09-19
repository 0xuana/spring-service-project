package com.example.department.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "departments", schema = "department")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    @Column(columnDefinition = "text")
    private String description;
}
