package com.example.project.client;

import com.example.project.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "EMPLOYEE-SERVICE", fallback = EmployeeClientFallback.class)
public interface EmployeeClient {

    /**
     * Get employee by ID for validation and enrichment
     */
    @GetMapping("/api/v1/employees/{id}")
    EmployeeDTO getEmployeeById(@PathVariable("id") Long id, @RequestParam(value = "enrich", defaultValue = "false") boolean enrich);

    /**
     * Get multiple employees by IDs for bulk enrichment
     */
    @GetMapping("/api/v1/employees/by-ids")
    List<EmployeeDTO> getEmployeesByIds(@RequestParam("ids") List<Long> ids);

    /**
     * Check if employee exists (lightweight operation)
     */
    @GetMapping("/api/v1/employees/{id}/exists")
    Boolean employeeExists(@PathVariable("id") Long id);
}