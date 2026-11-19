package com.epam.rd.autocode.assessment.appliances.dto.manufacturer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManufacturerDto {
    private Long id;
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String name;
}
