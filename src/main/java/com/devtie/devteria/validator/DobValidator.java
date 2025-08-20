package com.devtie.devteria.validator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DobValidator implements ConstraintValidator<DobConstraint, LocalDate> {
    private int min;

    @Override
    public void initialize(DobConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.min = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(LocalDate dob, ConstraintValidatorContext context) {
        if (dob == null) {
            return true; // Null values are considered valid, handled by @NotNull if needed
        }
        long years =
                ChronoUnit.YEARS.between(dob, LocalDate.now()); // thư viện ChronoUnit để tính số năm giữa ngày sinh
        // và ngày hiện tại
        return years >= min; // Check if the age is greater than or equal to the minimum age
    }
}
