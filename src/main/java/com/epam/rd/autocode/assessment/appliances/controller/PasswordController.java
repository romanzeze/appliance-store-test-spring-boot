package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.service.CurrentUser;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/password/change")
@RequiredArgsConstructor
public class PasswordController {

    private static final String VIEW = "auth/changePassword";

    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String form(Model model) {
        model.addAttribute("passwordForm", new ChangePasswordDto());
        model.addAttribute("forced", currentUserService.requireStoredUser().mustChangePassword());
        return VIEW;
    }

    @PostMapping
    public String change(@Valid @ModelAttribute("passwordForm") ChangePasswordDto dto, BindingResult result,
                         Model model, HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        CurrentUser current = currentUserService.requireStoredUser();
        model.addAttribute("forced", current.mustChangePassword());
        if (result.hasErrors()) {
            return VIEW;
        }
        try {
            if (current.isClient()) {
                clientService.changePassword(current.id(), dto, current.toActor());
            } else {
                employeeService.changePassword(current.id(), dto, current.toActor());
            }
        } catch (BusinessException exception) {
            result.rejectValue(fieldFor(exception), exception.getMessageKey(), exception.getMessageKey());
            return VIEW;
        }
        currentUserService.refresh(request, response);
        FlashMessages.success(redirectAttributes, "msg.password.changed");
        return "redirect:/";
    }

    private String fieldFor(BusinessException exception) {
        return "error.password.currentWrong".equals(exception.getMessageKey()) ? "currentPassword" : "newPassword";
    }
}
