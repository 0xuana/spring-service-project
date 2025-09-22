package com.example.project.validation;

import com.example.project.domain.Project;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Project> {

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Project project, ConstraintValidatorContext context) {
        if (project == null || project.getStartDate() == null) {
            return true; // Let other validations handle null values
        }

        if (project.getEndDate() == null) {
            return true; // End date is optional
        }

        return !project.getEndDate().isBefore(project.getStartDate());
    }
}