package com.epam.rd.autocode.assessment.appliances.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Abcdef1!", "Strong-Pass-1!", "Zz9#zzzz", "Пароль-Сильний1!"})
    void acceptsStrongPasswords(String password) {
        assertTrue(validator.isValid(password, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"alllowercase1!", "ALLUPPERCASE1!", "NoDigits-Here!", "NoSpecial123A", "Ab1!", "Sp ace 1A "})
    void rejectsWeakPasswords(String password) {
        assertFalse(validator.isValid(password, null));
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() {
        assertFalse(validator.isValid("Aa1!" + "x".repeat(70), null));
    }

    @Test
    void leavesEmptyValuesToNotBlank() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
    }
}
