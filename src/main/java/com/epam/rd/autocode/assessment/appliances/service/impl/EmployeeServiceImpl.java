package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeCreatedDto;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.exception.DuplicateEmailException;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import com.epam.rd.autocode.assessment.appliances.service.EmailAvailabilityService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.service.PasswordService;
import com.epam.rd.autocode.assessment.appliances.service.support.AccountStatusFilter;
import com.epam.rd.autocode.assessment.appliances.service.support.EmailNormalizer;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Loggable
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String ENTITY = "Employee";

    private final EmployeeRepository employeeRepository;
    private final EmailAvailabilityService emailAvailabilityService;
    private final PasswordService passwordService;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> findAll(String query, String status, Pageable pageable) {
        String text = StringUtils.hasText(query) ? query.trim() : "";
        return employeeRepository.filter(text, AccountStatusFilter.normalize(status), pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> findDeleted(Pageable pageable) {
        return employeeRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto findById(Long id) {
        return toDto(activeEmployee(id));
    }

    @Override
    public EmployeeCreatedDto create(EmployeeDto dto, Actor actor) {
        actor.requireAdmin();
        assertStaffRole(dto.getRole());
        String email = EmailNormalizer.normalize(dto.getEmail());
        if (emailAvailabilityService.isTaken(email, AccountType.EMPLOYEE, null)) {
            throw new DuplicateEmailException();
        }
        Employee employee = modelMapper.map(dto, Employee.class);
        employee.setId(null);
        employee.setEmail(email);
        employee.setRole(dto.getRole());
        String temporaryPassword = passwordService.generateTemporaryPassword();
        passwordService.applyTemporaryPassword(employee, temporaryPassword);
        Employee saved = employeeRepository.save(employee);
        auditService.record(actor, AuditAction.CREATED, ENTITY, saved.getId());
        log.info("Employee {} created by admin {}", saved.getId(), actor.id());
        return new EmployeeCreatedDto(toDto(saved), temporaryPassword);
    }

    @Override
    public EmployeeDto update(Long id, EmployeeDto dto, Actor actor) {
        actor.requireAdmin();
        Employee employee = activeEmployee(id);
        String email = EmailNormalizer.normalize(dto.getEmail());
        if (emailAvailabilityService.isTaken(email, AccountType.EMPLOYEE, id)) {
            throw new DuplicateEmailException();
        }
        dto.setId(id);
        modelMapper.map(dto, employee);
        employee.setEmail(email);
        Employee saved = employeeRepository.save(employee);
        auditService.record(actor, AuditAction.UPDATED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public EmployeeDto changeRole(Long id, Role role, Actor actor) {
        actor.requireAdmin();
        assertStaffRole(role);
        assertNotSelf(actor, id);
        Employee employee = activeEmployee(id);
        if (employee.getRole() == role) {
            return toDto(employee);
        }
        if (role != Role.ADMIN) {
            assertNotLastAdmin(employee);
        }
        employee.setRole(role);
        Employee saved = employeeRepository.save(employee);
        auditService.record(actor, AuditAction.ROLE_CHANGED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public String resetPassword(Long id, Actor actor) {
        actor.requireAdmin();
        Employee employee = activeEmployee(id);
        String temporaryPassword = passwordService.generateTemporaryPassword();
        passwordService.applyTemporaryPassword(employee, temporaryPassword);
        employeeRepository.save(employee);
        auditService.record(actor, AuditAction.PASSWORD_RESET, ENTITY, id);
        return temporaryPassword;
    }

    @Override
    public void changePassword(Long id, ChangePasswordDto dto, Actor actor) {
        if (!actor.isStaff() || !actor.id().equals(id)) {
            throw new AccessDeniedException("Only the account owner can change the password");
        }
        Employee employee = activeEmployee(id);
        passwordService.changePassword(employee, dto);
        employeeRepository.save(employee);
        auditService.record(actor, AuditAction.PASSWORD_CHANGED, ENTITY, id);
    }

    @Override
    public void block(Long id, Actor actor) {
        actor.requireAdmin();
        assertNotSelf(actor, id);
        Employee employee = activeEmployee(id);
        assertNotLastAdmin(employee);
        employee.setBlocked(true);
        employeeRepository.save(employee);
        auditService.record(actor, AuditAction.BLOCKED, ENTITY, id);
    }

    @Override
    public void unblock(Long id, Actor actor) {
        actor.requireAdmin();
        Employee employee = activeEmployee(id);
        employee.setBlocked(false);
        employeeRepository.save(employee);
        auditService.record(actor, AuditAction.UNBLOCKED, ENTITY, id);
    }

    @Override
    public void delete(Long id, Actor actor) {
        actor.requireAdmin();
        assertNotSelf(actor, id);
        Employee employee = activeEmployee(id);
        assertNotLastAdmin(employee);
        employeeRepository.delete(employee);
        auditService.record(actor, AuditAction.DELETED, ENTITY, id);
    }

    @Override
    public void restore(Long id, Actor actor) {
        actor.requireAdmin();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        if (!employee.isDeleted()) {
            return;
        }
        if (emailAvailabilityService.isTaken(employee.getEmail(), AccountType.EMPLOYEE, id)) {
            throw new DuplicateEmailException();
        }
        employeeRepository.restore(id);
        auditService.record(actor, AuditAction.RESTORED, ENTITY, id);
    }

    private void assertStaffRole(Role role) {
        if (role != Role.EMPLOYEE && role != Role.ADMIN) {
            throw new OperationNotAllowedException("error.employee.invalidRole");
        }
    }

    private void assertNotSelf(Actor actor, Long id) {
        if (actor.id().equals(id)) {
            throw new OperationNotAllowedException("error.account.self");
        }
    }

    private void assertNotLastAdmin(Employee employee) {
        if (employee.getRole() == Role.ADMIN && !employee.isBlocked()
                && employeeRepository.countByRoleAndDeletedFalseAndBlockedFalse(Role.ADMIN) <= 1) {
            throw new OperationNotAllowedException("error.account.lastAdmin");
        }
    }

    private Employee activeEmployee(Long id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private EmployeeDto toDto(Employee employee) {
        return modelMapper.map(employee, EmployeeDto.class);
    }
}
