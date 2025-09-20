package com.example.department.exception;

public class DuplicateCodeException extends RuntimeException {
    public DuplicateCodeException(String code) {
        super("Department with code already exists: " + code);
    }
}