package com.example.department.exception;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(Long id) {
        super("Department not found with id: " + id);
    }

    public DepartmentNotFoundException(String message) {
        super(message);
    }
}