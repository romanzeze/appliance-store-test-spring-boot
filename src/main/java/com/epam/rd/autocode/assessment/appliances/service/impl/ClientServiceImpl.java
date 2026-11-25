package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import com.epam.rd.autocode.assessment.appliances.exception.DuplicateEmailException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmailAvailabilityService;
import com.epam.rd.autocode.assessment.appliances.service.PasswordService;
import com.epam.rd.autocode.assessment.appliances.service.support.AccountStatusFilter;
import com.epam.rd.autocode.assessment.appliances.service.support.EmailNormalizer;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Loggable
@Service
@Transactional
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private static final String ENTITY = "Client";

    private final ClientRepository clientRepository;
    private final EmailAvailabilityService emailAvailabilityService;
    private final PasswordService passwordService;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDto> findAll(String query, String status, Pageable pageable) {
        String text = StringUtils.hasText(query) ? query.trim() : "";
        return clientRepository.filter(text, AccountStatusFilter.normalize(status), pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDto> findDeleted(Pageable pageable) {
        return clientRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto findById(Long id) {
        return toDto(activeClient(id));
    }

    @Override
    public ClientDto register(RegistrationDto dto) {
        String email = EmailNormalizer.normalize(dto.getEmail());
        if (emailAvailabilityService.isTaken(email, AccountType.CLIENT, null)) {
            throw new DuplicateEmailException();
        }
        Client client = modelMapper.map(dto, Client.class);
        client.setEmail(email);
        client.setPassword(passwordService.encode(dto.getPassword()));
        client.setRole(Role.USER);
        Client saved = clientRepository.save(client);
        auditService.record(new Actor(saved.getId(), Role.USER), AuditAction.CREATED, ENTITY, saved.getId());
        log.info("Client {} registered", saved.getId());
        return toDto(saved);
    }

    @Override
    public ClientDto update(Long id, ClientDto dto, Actor actor) {
        requireSelfOrStaff(actor, id);
        Client client = activeClient(id);
        String email = EmailNormalizer.normalize(dto.getEmail());
        if (emailAvailabilityService.isTaken(email, AccountType.CLIENT, id)) {
            throw new DuplicateEmailException();
        }
        dto.setId(id);
        modelMapper.map(dto, client);
        client.setEmail(email);
        Client saved = clientRepository.save(client);
        auditService.record(actor, AuditAction.UPDATED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public void changePassword(Long id, ChangePasswordDto dto, Actor actor) {
        if (!actor.is(Role.USER, id)) {
            throw new AccessDeniedException("Only the account owner can change the password");
        }
        Client client = activeClient(id);
        passwordService.changePassword(client, dto);
        clientRepository.save(client);
        auditService.record(actor, AuditAction.PASSWORD_CHANGED, ENTITY, id);
    }

    @Override
    public void block(Long id, Actor actor) {
        actor.requireStaff();
        Client client = activeClient(id);
        client.setBlocked(true);
        clientRepository.save(client);
        auditService.record(actor, AuditAction.BLOCKED, ENTITY, id);
    }

    @Override
    public void unblock(Long id, Actor actor) {
        actor.requireStaff();
        Client client = activeClient(id);
        client.setBlocked(false);
        clientRepository.save(client);
        auditService.record(actor, AuditAction.UNBLOCKED, ENTITY, id);
    }

    @Override
    public void deactivate(Long id, Actor actor) {
        requireSelfOrStaff(actor, id);
        clientRepository.delete(activeClient(id));
        auditService.record(actor, AuditAction.ACCOUNT_DEACTIVATED, ENTITY, id);
    }

    @Override
    public void restore(Long id, Actor actor) {
        actor.requireStaff();
        Client client = clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        if (!client.isDeleted()) {
            return;
        }
        if (emailAvailabilityService.isTaken(client.getEmail(), AccountType.CLIENT, id)) {
            throw new DuplicateEmailException();
        }
        clientRepository.restore(id);
        auditService.record(actor, AuditAction.RESTORED, ENTITY, id);
    }

    @Override
    public void eraseAllData(Long id, Actor actor) {
        requireSelfOrStaff(actor, id);
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException(ENTITY, id);
        }
        auditService.record(actor, AuditAction.ACCOUNT_ERASED, ENTITY, id);
        clientRepository.hardDeleteById(id);
        log.info("All data of client {} erased", id);
    }

    private void requireSelfOrStaff(Actor actor, Long id) {
        if (!actor.isStaff() && !actor.is(Role.USER, id)) {
            throw new AccessDeniedException("Only the account owner or staff can perform this action");
        }
    }

    private Client activeClient(Long id) {
        return clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private ClientDto toDto(Client client) {
        return modelMapper.map(client, ClientDto.class);
    }
}
