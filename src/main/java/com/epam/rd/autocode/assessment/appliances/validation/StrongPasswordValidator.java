package com.epam.rd.autocode.assessment.appliances.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char symbol : password.toCharArray()) {
            if (Character.isUpperCase(symbol)) {
                upper = true;
            } else if (Character.isLowerCase(symbol)) {
                lower = true;
            } else if (Character.isDigit(symbol)) {
                digit = true;
            } else if (!Character.isWhitespace(symbol)) {
                special = true;
            }
        }
        return upper && lower && digit && special;
    }
}
