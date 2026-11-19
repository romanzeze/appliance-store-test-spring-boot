package com.epam.rd.autocode.assessment.appliances.validation;

import com.epam.rd.autocode.assessment.appliances.service.EmailAvailabilityService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, EmailSubject> {

    private final EmailAvailabilityService emailAvailabilityService;

    @Override
    public boolean isValid(EmailSubject subject, ConstraintValidatorContext context) {
        String email = subject.getEmail();
        if (email == null || email.isBlank() || !emailAvailabilityService.isTaken(email.trim(), subject.accountType(), subject.getId())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("email")
                .addConstraintViolation();
        return false;
    }
}
