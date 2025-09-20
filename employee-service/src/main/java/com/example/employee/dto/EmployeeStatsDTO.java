package com.example.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeStatsDTO {
    private long totalEmployees;
    private List<DepartmentCount> countsByDepartment;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepartmentCount {
        private Long departmentId;
        private Long count;
        private String departmentName; // Optional enrichment
    }
}