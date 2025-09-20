package com.example.department.exception;

public class DepartmentInUseException extends RuntimeException {
    public DepartmentInUseException(Long departmentId, long employeeCount) {
        super(String.format("Cannot delete department %d: %d employees are still assigned. " +
                          "Please reassign or remove these employees before deleting the department.",
                          departmentId, employeeCount));
    }
}