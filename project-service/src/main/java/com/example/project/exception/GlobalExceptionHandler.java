package com.example.project.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for one or more fields"
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();

        problemDetail.setProperty("errors", errors);

        logger.warn("Validation error for request: {} - traceId: {}",
            request.getDescription(false), traceId);

        return problemDetail;
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleProjectNotFound(ProjectNotFoundException ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/not-found"));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        logger.warn("Project not found: {} - traceId: {}", ex.getMessage(), traceId);

        return problemDetail;
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ProblemDetail handleEmployeeNotFound(EmployeeNotFoundException ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/employee-not-found"));
        problemDetail.setTitle("Referenced Employee Not Found");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        logger.warn("Employee not found for project operation: {} - traceId: {}", ex.getMessage(), traceId);

        return problemDetail;
    }

    @ExceptionHandler(DuplicateCodeException.class)
    public ProblemDetail handleDuplicateCode(DuplicateCodeException ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/duplicate-code"));
        problemDetail.setTitle("Duplicate Project Code");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        logger.warn("Duplicate project code conflict: {} - traceId: {}", ex.getMessage(), traceId);

        return problemDetail;
    }

    @ExceptionHandler(DuplicateMemberException.class)
    public ProblemDetail handleDuplicateMember(DuplicateMemberException ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/duplicate-member"));
        problemDetail.setTitle("Duplicate Project Member");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        logger.warn("Duplicate project member conflict: {} - traceId: {}", ex.getMessage(), traceId);

        return problemDetail;
    }

    @ExceptionHandler(ProjectHasMembersException.class)
    public ProblemDetail handleProjectHasMembers(ProjectHasMembersException ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/resource-in-use"));
        problemDetail.setTitle("Project Has Active Members");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));
        problemDetail.setProperty("hint", "Remove all project members before deleting the project, or use cascade delete if supported.");

        logger.warn("Project deletion blocked due to active members: {} - traceId: {}", ex.getMessage(), traceId);

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        String traceId = getOrGenerateTraceId();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later."
        );

        problemDetail.setType(URI.create("https://api.company.com/problems/internal-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("instance", request.getDescription(false).replace("uri=", ""));

        logger.error("Unexpected error occurred - traceId: {}", traceId, ex);

        return problemDetail;
    }

    private String getOrGenerateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }
}