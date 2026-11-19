package com.epam.rd.autocode.assessment.appliances.dto.employee;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import com.epam.rd.autocode.assessment.appliances.validation.EmailSubject;
import com.epam.rd.autocode.assessment.appliances.validation.UniqueEmail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@UniqueEmail
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto implements EmailSubject {
    private Long id;
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String name;
    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 255, message = "{validation.max.length}")
    private String email;
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String department;
    @NotNull(message = "{validation.required}")
    private Role role;
    private boolean blocked;
    private boolean mustChangePassword;

    @Override
    public AccountType accountType() {
        return AccountType.EMPLOYEE;
    }
}
