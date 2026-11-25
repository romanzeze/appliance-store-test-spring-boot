package com.epam.rd.autocode.assessment.appliances.dto.contact;

import jakarta.validation.constraints.Email;
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
public class ContactDto {
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String name;
    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 255, message = "{validation.max.length}")
    private String email;
    @NotBlank(message = "{validation.required}")
    @Size(max = 2000, message = "{validation.max.length}")
    private String message;
}
