package com.epam.rd.autocode.assessment.appliances.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String ERROR_VIEW_4XX = "error/4xx";

    private final MessageSource messageSource;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessageKey());
        return errorView(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException exception) {
        log.warn("Business rule violated: {}", exception.getMessageKey());
        return errorView(HttpStatus.CONFLICT, exception);
    }

    private ModelAndView errorView(HttpStatus status, BusinessException exception) {
        String message = messageSource.getMessage(exception.getMessageKey(), exception.getArgs(),
                exception.getMessageKey(), LocaleContextHolder.getLocale());
        ModelAndView view = new ModelAndView(ERROR_VIEW_4XX);
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        return view;
    }
}
