package com.epam.rd.autocode.assessment.appliances.exception;

public class OperationNotAllowedException extends BusinessException {

    public OperationNotAllowedException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
