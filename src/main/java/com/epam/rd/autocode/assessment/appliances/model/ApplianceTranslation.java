package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appliance_translation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplianceTranslation {

    public static final String UK = "uk";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appliance_id", nullable = false)
    private Long applianceId;

    @Column(nullable = false)
    private String language;

    private String name;

    private String characteristic;

    private String description;
}
