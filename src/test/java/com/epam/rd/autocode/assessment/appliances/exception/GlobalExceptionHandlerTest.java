package com.epam.rd.autocode.assessment.appliances.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ThrowingController.class})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
class GlobalExceptionHandlerTest {

    @Controller
    static class ThrowingController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Appliance", 7L);
        }

        @GetMapping("/test/duplicate")
        String duplicate() {
            throw new DuplicateEmailException();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundReturns404PageWithEnglishMessage() throws Exception {
        mockMvc.perform(get("/test/not-found").header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/4xx"))
                .andExpect(model().attribute("message", "Appliance with id 7 was not found"))
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    void notFoundMessageIsTranslated() throws Exception {
        mockMvc.perform(get("/test/not-found").header("Accept-Language", "uk"))
                .andExpect(status().isNotFound())
                .andExpect(model().attribute("message", "Не знайдено: Appliance з id 7"))
                .andExpect(content().string(containsString("Сторінку не знайдено")));
    }

    @Test
    void businessErrorReturns409() throws Exception {
        mockMvc.perform(get("/test/duplicate").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/4xx"))
                .andExpect(content().string(containsString("This email is already registered")));
    }
}
