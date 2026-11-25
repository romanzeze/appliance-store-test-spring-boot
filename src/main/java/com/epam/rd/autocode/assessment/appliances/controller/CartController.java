package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.Cart;
import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final String DEFAULT_NEXT = "/appliances";

    private final Cart cart;
    private final ApplianceService applianceService;

    @PostMapping("/add")
    public String add(@RequestParam Long applianceId, @RequestParam(required = false) String next,
                      RedirectAttributes redirectAttributes) {
        applianceService.findById(applianceId);
        cart.add(applianceId);
        FlashMessages.success(redirectAttributes, "msg.cart.added");
        return "redirect:" + safe(next);
    }

    @PostMapping("/clear")
    public String clear(RedirectAttributes redirectAttributes) {
        cart.clear();
        FlashMessages.success(redirectAttributes, "msg.cart.cleared");
        return "redirect:" + DEFAULT_NEXT;
    }

    private String safe(String next) {
        boolean local = next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")
                && !next.contains("\r") && !next.contains("\n");
        return local ? next : DEFAULT_NEXT;
    }
}
