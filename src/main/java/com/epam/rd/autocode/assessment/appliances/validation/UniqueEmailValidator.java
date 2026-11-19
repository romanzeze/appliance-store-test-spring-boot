package com.epam.rd.autocode.assessment.appliances.validation;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, EmailSubject> {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public boolean isValid(EmailSubject subject, ConstraintValidatorContext context) {
        String email = subject.getEmail();
        if (email == null || email.isBlank() || !isTaken(subject, email.trim())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("email")
                .addConstraintViolation();
        return false;
    }

    private boolean isTaken(EmailSubject subject, String email) {
        Optional<Client> client = clientRepository.findByEmailIgnoreCaseAndDeletedFalse(email);
        Optional<Employee> employee = employeeRepository.findByEmailIgnoreCaseAndDeletedFalse(email);
        if (subject.accountType() == AccountType.CLIENT) {
            return employee.isPresent() || client.filter(found -> !Objects.equals(found.getId(), subject.getId())).isPresent();
        }
        return client.isPresent() || employee.filter(found -> !Objects.equals(found.getId(), subject.getId())).isPresent();
    }
}
