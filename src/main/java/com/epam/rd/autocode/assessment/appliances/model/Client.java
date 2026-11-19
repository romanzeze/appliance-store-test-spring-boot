package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

@Entity
@SQLDelete(sql = "UPDATE client SET deleted = true WHERE id = ?")
@Getter
@Setter
public class Client extends User {
    @NotBlank
    private String card;

    public Client() {
        setRole(Role.USER);
    }

    public Client(Long id, String name, String email, String password, String card) {
        super(id, name, email, password);
        this.card = card;
        setRole(Role.USER);
    }
}
