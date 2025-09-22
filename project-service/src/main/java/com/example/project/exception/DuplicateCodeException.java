package com.example.project.exception;

public class DuplicateCodeException extends RuntimeException {
    public DuplicateCodeException(String message) {
        super(message);
    }

    public static DuplicateCodeException forCode(String code) {
        return new DuplicateCodeException("Project with code '" + code + "' already exists");
    }
}