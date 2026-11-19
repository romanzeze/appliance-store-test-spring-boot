package com.epam.rd.autocode.assessment.appliances.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long employeeId;
    private String employeeName;
    private Boolean approved;
    @Valid
    @NotEmpty(message = "{validation.rows.required}")
    @Builder.Default
    private List<OrderRowDto> rows = new ArrayList<>();
    private BigDecimal totalAmount;
}
