package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.dto.contact.ContactDto;
import com.epam.rd.autocode.assessment.appliances.service.CurrentUser;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private static final String VIEW = "contact/form";

    private final ContactService contactService;
    private final CurrentUserService currentUserService;

    @InitBinder("contact")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "message");
    }

    @GetMapping
    public String form(Model model) {
        ContactDto dto = new ContactDto();
        currentUserService.findUser().ifPresent(user -> {
            dto.setName(user.name());
            dto.setEmail(user.email());
        });
        model.addAttribute("contact", dto);
        return VIEW;
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("contact") ContactDto dto, BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return VIEW;
        }
        Long clientId = currentUserService.findUser()
                .filter(CurrentUser::isClient)
                .map(CurrentUser::id)
                .orElse(null);
        contactService.submit(dto, clientId);
        FlashMessages.success(redirectAttributes, "msg.contact.sent");
        return "redirect:/contact";
    }
}
