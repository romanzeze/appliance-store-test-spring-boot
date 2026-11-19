package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

@Entity
@SQLDelete(sql = "UPDATE appliance SET deleted = true WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Appliance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    @Enumerated(EnumType.STRING)
    @NotNull
    private Category category;
    @NotBlank
    private String model;
    @ManyToOne
    @NotNull
    private Manufacturer manufacturer;
    @Enumerated(EnumType.STRING)
    @NotNull
    private PowerType powerType;
    private String characteristic;
    private String description;
    @NotNull
    @Positive
    private Integer power;
    @NotNull
    @Positive
    private BigDecimal price;
}
