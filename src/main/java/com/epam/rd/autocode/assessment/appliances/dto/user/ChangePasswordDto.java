package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.constraints.NotBlank;
import com.epam.rd.autocode.assessment.appliances.validation.PasswordsMatch;
import com.epam.rd.autocode.assessment.appliances.validation.StrongPassword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PasswordsMatch(field = "newPassword")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDto {
    @NotBlank(message = "{validation.required}")
    private String currentPassword;
    @NotBlank(message = "{validation.required}")
    @StrongPassword
    private String newPassword;
    @NotBlank(message = "{validation.required}")
    private String confirmPassword;
}
