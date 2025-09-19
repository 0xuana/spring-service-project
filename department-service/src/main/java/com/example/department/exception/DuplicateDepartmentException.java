package com.example.department.exception;

public class DuplicateDepartmentException extends RuntimeException {
    public DuplicateDepartmentException(String name) {
        super("Department with name already exists: " + name);
    }
}