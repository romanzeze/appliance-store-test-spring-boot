package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.security.AppUserDetails;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private static final String VIEW = "account/profile";

    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    @InitBinder("client")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "card");
    }

    @ModelAttribute("client")
    ClientDto client() {
        AppUserDetails current = currentUserService.requireCurrent();
        return current.getAccountType() == AccountType.CLIENT ? clientService.findById(current.getId()) : null;
    }

    @GetMapping
    public String profile(Model model) {
        AppUserDetails current = currentUserService.requireCurrent();
        if (current.getAccountType() == AccountType.EMPLOYEE) {
            model.addAttribute("employee", employeeService.findById(current.getId()));
        }
        return VIEW;
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("client") ClientDto dto, BindingResult result,
                         HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        AppUserDetails current = requireClient();
        if (result.hasErrors()) {
            return VIEW;
        }
        try {
            clientService.update(current.getId(), dto, current.toActor());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return VIEW;
        }
        currentUserService.refresh(request, response);
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:/account";
    }

    @PostMapping("/deactivate")
    public String deactivate(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        AppUserDetails current = requireClient();
        clientService.deactivate(current.getId(), current.toActor());
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?deleted";
    }

    @PostMapping("/erase")
    public String erase(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        AppUserDetails current = requireClient();
        clientService.eraseAllData(current.getId(), current.toActor());
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?erased";
    }

    private AppUserDetails requireClient() {
        AppUserDetails current = currentUserService.requireCurrent();
        if (current.getAccountType() != AccountType.CLIENT) {
            throw new AccessDeniedException("Only clients can manage their own profile here");
        }
        return current;
    }
}
