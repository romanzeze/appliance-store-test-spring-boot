package com.epam.rd.autocode.assessment.appliances.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {

    String message() default "{validation.password.mismatch}";

    String field() default "password";

    String confirmField() default "confirmPassword";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
