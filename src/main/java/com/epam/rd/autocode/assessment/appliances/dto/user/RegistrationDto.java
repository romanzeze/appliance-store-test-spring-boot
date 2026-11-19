package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import com.epam.rd.autocode.assessment.appliances.validation.EmailSubject;
import com.epam.rd.autocode.assessment.appliances.validation.PasswordsMatch;
import com.epam.rd.autocode.assessment.appliances.validation.StrongPassword;
import com.epam.rd.autocode.assessment.appliances.validation.UniqueEmail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@UniqueEmail
@PasswordsMatch
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDto implements EmailSubject {
    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max.length}")
    private String name;
    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 255, message = "{validation.max.length}")
    private String email;
    @NotBlank(message = "{validation.required}")
    @StrongPassword
    private String password;
    @NotBlank(message = "{validation.required}")
    private String confirmPassword;
    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^[0-9 -]{8,23}$", message = "{validation.card.invalid}")
    private String card;

    @Override
    public AccountType accountType() {
        return AccountType.CLIENT;
    }
}
