package com.epam.rd.autocode.assessment.appliances.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super("error.resource.notFound", resource, id);
    }
}
