package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeCreatedDto;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    Page<EmployeeDto> findAll(String query, String status, Pageable pageable);

    Page<EmployeeDto> findDeleted(Pageable pageable);

    EmployeeDto findById(Long id);

    EmployeeCreatedDto create(EmployeeDto dto, Actor actor);

    EmployeeDto update(Long id, EmployeeDto dto, Actor actor);

    EmployeeDto changeRole(Long id, Role role, Actor actor);

    String resetPassword(Long id, Actor actor);

    void changePassword(Long id, ChangePasswordDto dto, Actor actor);

    void block(Long id, Actor actor);

    void unblock(Long id, Actor actor);

    void delete(Long id, Actor actor);

    void restore(Long id, Actor actor);
}
