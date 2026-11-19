package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class Account extends BaseEntity {
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean blocked;
    private boolean mustChangePassword;
    private LocalDateTime passwordExpiresAt;
}
