package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final String LOGIN_VIEW = "auth/login";
    private static final String REGISTER_VIEW = "auth/register";

    private final ClientService clientService;

    @GetMapping("/login")
    public String login() {
        return LOGIN_VIEW;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registration", new RegistrationDto());
        return REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return REGISTER_VIEW;
        }
        try {
            clientService.register(dto);
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return REGISTER_VIEW;
        }
        return "redirect:/login?registered";
    }
}
