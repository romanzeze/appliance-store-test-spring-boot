package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.security.InMemoryAccountService;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailAvailabilityService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final InMemoryAccountService inMemoryAccountService;

    @Transactional(readOnly = true)
    public boolean isTaken(String email, AccountType accountType, Long ownId) {
        if (inMemoryAccountService.isReserved(email)) {
            return true;
        }
        Optional<Client> client = clientRepository.findByEmailIgnoreCaseAndDeletedFalse(email);
        Optional<Employee> employee = employeeRepository.findByEmailIgnoreCaseAndDeletedFalse(email);
        if (accountType == AccountType.CLIENT) {
            return employee.isPresent() || client.filter(found -> !Objects.equals(found.getId(), ownId)).isPresent();
        }
        return client.isPresent() || employee.filter(found -> !Objects.equals(found.getId(), ownId)).isPresent();
    }
}
