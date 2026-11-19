package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplianceDto {
    private Long id;
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String name;
    @NotNull(message = "{validation.required}")
    private Category category;
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String model;
    @NotNull(message = "{validation.required}")
    private Long manufacturerId;
    private String manufacturerName;
    @NotNull(message = "{validation.required}")
    private PowerType powerType;
    @Size(max = 255, message = "{validation.max.length}")
    private String characteristic;
    @Size(max = 1000, message = "{validation.max.length}")
    private String description;
    @NotNull(message = "{validation.required}")
    @Positive(message = "{validation.positive}")
    private Integer power;
    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.01", message = "{validation.positive}")
    @Digits(integer = 17, fraction = 2, message = "{validation.price.format}")
    private BigDecimal price;
}
