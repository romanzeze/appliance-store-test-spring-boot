package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeCreatedDto;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.exception.DuplicateEmailException;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.service.impl.EmployeeServiceImpl;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import com.epam.rd.autocode.assessment.appliances.validation.StrongPasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final Actor ADMIN = new Actor(1L, Role.ADMIN);
    private static final Actor STAFF = new Actor(2L, Role.EMPLOYEE);

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmailAvailabilityService emailAvailabilityService;
    @Mock
    private AuditService auditService;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(employeeRepository, emailAvailabilityService, ServiceTestSupport.passwordService(),
                ServiceTestSupport.mapper(), auditService);
    }

    private Employee employee(Long id, Role role) {
        Employee employee = new Employee(id, "Michael Jordan", "mj@example.com",
                ServiceTestSupport.encoder().encode("Old-Pass-1!"), "sales");
        employee.setRole(role);
        return employee;
    }

    @Test
    void adminCreatesEmployeeWithTemporaryPassword() {
        when(emailAvailabilityService.isTaken("mj@example.com", AccountType.EMPLOYEE, null)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(call -> {
            Employee saved = call.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        EmployeeDto dto = EmployeeDto.builder().name("Michael Jordan").email("MJ@example.com").department("sales")
                .role(Role.EMPLOYEE).build();

        EmployeeCreatedDto result = service.create(dto, ADMIN);

        assertEquals(9L, result.getEmployee().getId());
        assertTrue(new StrongPasswordValidator().isValid(result.getTemporaryPassword(), null));
        assertTrue(result.getEmployee().isMustChangePassword());
        verify(employeeRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getRole() == Role.EMPLOYEE
                        && saved.isMustChangePassword()
                        && saved.getEmail().equals("mj@example.com")
                        && saved.getPasswordExpiresAt().equals(LocalDateTime.of(2026, 11, 21, 10, 0))
                        && ServiceTestSupport.encoder().matches(result.getTemporaryPassword(), saved.getPassword())));
        verify(auditService).record(ADMIN, AuditAction.CREATED, "Employee", 9L);
    }

    @Test
    void onlyAdminCanCreateEmployees() {
        EmployeeDto dto = EmployeeDto.builder().email("a@b.com").role(Role.EMPLOYEE).build();

        assertThrows(AccessDeniedException.class, () -> service.create(dto, STAFF));
    }

    @Test
    void createRejectsClientRole() {
        EmployeeDto dto = EmployeeDto.builder().email("a@b.com").role(Role.USER).build();

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.create(dto, ADMIN));

        assertEquals("error.employee.invalidRole", exception.getMessageKey());
    }

    @Test
    void createRejectsTakenEmail() {
        when(emailAvailabilityService.isTaken("mj@example.com", AccountType.EMPLOYEE, null)).thenReturn(true);
        EmployeeDto dto = EmployeeDto.builder().email("mj@example.com").role(Role.EMPLOYEE).build();

        assertThrows(DuplicateEmailException.class, () -> service.create(dto, ADMIN));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateDoesNotChangeRoleOrPassword() {
        Employee existing = employee(5L, Role.EMPLOYEE);
        String passwordHash = existing.getPassword();
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));
        when(emailAvailabilityService.isTaken("mj@example.com", AccountType.EMPLOYEE, 5L)).thenReturn(false);
        when(employeeRepository.save(existing)).thenReturn(existing);
        EmployeeDto dto = EmployeeDto.builder().name("MJ").email("mj@example.com").department("security").role(Role.ADMIN)
                .build();

        EmployeeDto result = service.update(5L, dto, ADMIN);

        assertEquals("security", result.getDepartment());
        assertEquals(Role.EMPLOYEE, existing.getRole());
        assertEquals(passwordHash, existing.getPassword());
    }

    @Test
    void changeRoleEndsSessions() {
        Employee existing = employee(5L, Role.EMPLOYEE);
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeDto result = service.changeRole(5L, Role.ADMIN, ADMIN);

        assertEquals(Role.ADMIN, result.getRole());
        verify(auditService).record(ADMIN, AuditAction.ROLE_CHANGED, "Employee", 5L);
    }

    @Test
    void adminCannotChangeOwnRole() {
        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.changeRole(1L, Role.EMPLOYEE, ADMIN));

        assertEquals("error.account.self", exception.getMessageKey());
    }

    @Test
    void lastAdminCannotBeDemoted() {
        Employee lastAdmin = employee(5L, Role.ADMIN);
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(lastAdmin));
        when(employeeRepository.countByRoleAndDeletedFalseAndBlockedFalse(Role.ADMIN)).thenReturn(1L);

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.changeRole(5L, Role.EMPLOYEE, ADMIN));

        assertEquals("error.account.lastAdmin", exception.getMessageKey());
    }

    @Test
    void resetPasswordReturnsNewTemporaryPasswordAndEndsSessions() {
        Employee existing = employee(5L, Role.EMPLOYEE);
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        String temporary = service.resetPassword(5L, ADMIN);

        assertTrue(ServiceTestSupport.encoder().matches(temporary, existing.getPassword()));
        assertTrue(existing.isMustChangePassword());
        verify(auditService).record(ADMIN, AuditAction.PASSWORD_RESET, "Employee", 5L);
    }

    @Test
    void blockEndsSessions() {
        Employee existing = employee(5L, Role.EMPLOYEE);
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        service.block(5L, ADMIN);

        assertTrue(existing.isBlocked());
        verify(auditService).record(ADMIN, AuditAction.BLOCKED, "Employee", 5L);
    }

    @Test
    void adminCannotBlockSelf() {
        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.block(1L, ADMIN));

        assertEquals("error.account.self", exception.getMessageKey());
    }

    @Test
    void lastActiveAdminCannotBeBlockedButAnotherAdminCanBe() {
        Employee admin = employee(5L, Role.ADMIN);
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(admin));
        when(employeeRepository.countByRoleAndDeletedFalseAndBlockedFalse(Role.ADMIN)).thenReturn(1L, 2L);

        assertThrows(OperationNotAllowedException.class, () -> service.block(5L, ADMIN));
        service.block(5L, ADMIN);

        assertTrue(admin.isBlocked());
    }

    @Test
    void deleteIsSoftAndEndsSessions() {
        Employee existing = employee(5L, Role.EMPLOYEE);
        when(employeeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        service.delete(5L, ADMIN);

        verify(employeeRepository).delete(existing);
        verify(auditService).record(ADMIN, AuditAction.DELETED, "Employee", 5L);
    }

    @Test
    void adminCannotDeleteSelf() {
        assertThrows(OperationNotAllowedException.class, () -> service.delete(1L, ADMIN));
        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void restoreFailsWhenEmailWasTakenInTheMeantime() {
        Employee deleted = employee(5L, Role.EMPLOYEE);
        deleted.setDeleted(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(deleted));
        when(emailAvailabilityService.isTaken("mj@example.com", AccountType.EMPLOYEE, 5L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.restore(5L, ADMIN));
    }

    @Test
    void ownerChangesPasswordAndOthersCannot() {
        Employee existing = employee(2L, Role.EMPLOYEE);
        existing.setMustChangePassword(true);
        when(employeeRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(existing));

        service.changePassword(2L, new ChangePasswordDto("Old-Pass-1!", "New-Pass-2@", "New-Pass-2@"), STAFF);

        assertFalse(existing.isMustChangePassword());
        assertTrue(ServiceTestSupport.encoder().matches("New-Pass-2@", existing.getPassword()));
        assertThrows(AccessDeniedException.class,
                () -> service.changePassword(2L, new ChangePasswordDto("a", "b", "b"), new Actor(3L, Role.EMPLOYEE)));
        assertThrows(AccessDeniedException.class,
                () -> service.changePassword(2L, new ChangePasswordDto("a", "b", "b"), new Actor(2L, Role.USER)));
    }
}
