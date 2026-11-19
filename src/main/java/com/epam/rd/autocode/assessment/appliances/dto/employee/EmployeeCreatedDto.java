package com.epam.rd.autocode.assessment.appliances.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreatedDto {
    private EmployeeDto employee;
    private String temporaryPassword;
}
