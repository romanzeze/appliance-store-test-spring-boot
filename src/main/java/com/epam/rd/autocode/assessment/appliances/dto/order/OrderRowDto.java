package com.epam.rd.autocode.assessment.appliances.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRowDto {
    private Long id;
    @NotNull(message = "{validation.required}")
    private Long applianceId;
    private String applianceName;
    @NotNull(message = "{validation.required}")
    @Min(value = 1, message = "{validation.number.min}")
    private Long number;
    private BigDecimal amount;
}
