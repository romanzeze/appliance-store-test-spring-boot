package com.epam.rd.autocode.assessment.appliances.exception;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super("error.email.taken");
    }
}
