package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.contact.ContactDto;
import com.epam.rd.autocode.assessment.appliances.model.ContactMessage;
import com.epam.rd.autocode.assessment.appliances.repository.ContactMessageRepository;
import com.epam.rd.autocode.assessment.appliances.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Loggable
@Service
@Transactional
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMessageRepository contactMessageRepository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final Clock clock;

    @Value("${app.contact.recipient:}")
    private String recipient;

    @Value("${app.contact.from:no-reply@appliance.store}")
    private String from;

    @Override
    public boolean submit(ContactDto dto, Long clientId) {
        ContactMessage saved = contactMessageRepository.save(ContactMessage.builder()
                .createdAt(LocalDateTime.now(clock))
                .clientId(clientId)
                .name(dto.getName().trim())
                .email(dto.getEmail().trim())
                .message(dto.getMessage().trim())
                .build());
        boolean delivered = deliver(saved);
        saved.setDelivered(delivered);
        log.info("Contact message {} stored, delivered={}", saved.getId(), delivered);
        return delivered;
    }

    private boolean deliver(ContactMessage contact) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || recipient == null || recipient.isBlank()) {
            return false;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(recipient);
        mail.setReplyTo(contact.getEmail());
        mail.setSubject("Appliance store: message from " + contact.getName());
        mail.setText("From: " + contact.getName() + " <" + contact.getEmail() + ">\n\n" + contact.getMessage());
        try {
            sender.send(mail);
            return true;
        } catch (MailException exception) {
            log.warn("Contact message {} was not delivered: {}", contact.getId(), exception.getClass().getSimpleName());
            return false;
        }
    }
}
