package com.example.project.client;

import com.example.project.dto.EmployeeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class EmployeeClientFallback implements EmployeeClient {

    @Override
    public EmployeeDTO getEmployeeById(Long id, boolean enrich) {
        log.warn("Employee service is unavailable. Falling back for employee ID: {}", id);
        return null;
    }

    @Override
    public List<EmployeeDTO> getEmployeesByIds(List<Long> ids) {
        log.warn("Employee service is unavailable. Falling back for employee IDs: {}", ids);
        return Collections.emptyList();
    }

    @Override
    public Boolean employeeExists(Long id) {
        log.warn("Employee service is unavailable. Falling back for employee existence check: {}", id);
        // Conservative approach: assume employee doesn't exist when service is down
        return false;
    }
}