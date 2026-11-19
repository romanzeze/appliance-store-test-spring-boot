package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

@Entity
@SQLDelete(sql = "UPDATE employee SET deleted = true WHERE id = ?")
@Getter
@Setter
public class Employee extends User {
    @NotBlank
    private String department;

    public Employee() {
        setRole(Role.EMPLOYEE);
    }

    public Employee(Long id, String name, String email, String password, String department) {
        super(id, name, email, password);
        this.department = department;
        setRole(Role.EMPLOYEE);
    }
}
