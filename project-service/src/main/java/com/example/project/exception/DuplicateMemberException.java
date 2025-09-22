package com.example.project.exception;

public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException(String message) {
        super(message);
    }

    public static DuplicateMemberException forProjectAndEmployee(Long projectId, Long employeeId) {
        return new DuplicateMemberException("Employee " + employeeId + " is already a member of project " + projectId);
    }
}