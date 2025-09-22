package com.example.project.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }

    public static EmployeeNotFoundException forId(Long employeeId) {
        return new EmployeeNotFoundException("Employee not found with id: " + employeeId);
    }
}