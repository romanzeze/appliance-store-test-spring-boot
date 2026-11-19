package com.epam.rd.autocode.assessment.appliances.controller.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlashMessages {

    public static void success(RedirectAttributes redirectAttributes, String messageKey) {
        redirectAttributes.addFlashAttribute("successKey", messageKey);
    }
}
