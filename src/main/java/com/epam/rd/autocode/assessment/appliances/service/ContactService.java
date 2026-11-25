package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.contact.ContactDto;

public interface ContactService {

    boolean submit(ContactDto dto, Long clientId);
}
