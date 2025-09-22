package com.example.project.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }

    public ProjectNotFoundException(Long id) {
        super("Project not found with id: " + id);
    }
}