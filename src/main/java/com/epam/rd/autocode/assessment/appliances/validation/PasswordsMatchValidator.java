package com.epam.rd.autocode.assessment.appliances.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {

    private String field;
    private String confirmField;

    @Override
    public void initialize(PasswordsMatch annotation) {
        this.field = annotation.field();
        this.confirmField = annotation.confirmField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object password = wrapper.getPropertyValue(field);
        Object confirmation = wrapper.getPropertyValue(confirmField);
        if (password == null || confirmation == null || Objects.equals(password, confirmation)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(confirmField)
                .addConstraintViolation();
        return false;
    }
}
