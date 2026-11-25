package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Loggable
@Service
@Transactional
@RequiredArgsConstructor
public class ManufacturerServiceImpl implements ManufacturerService {

    private static final String ENTITY = "Manufacturer";

    private final ManufacturerRepository manufacturerRepository;
    private final ApplianceRepository applianceRepository;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<ManufacturerDto> findAll(String query, Pageable pageable) {
        Page<Manufacturer> page = StringUtils.hasText(query)
                ? manufacturerRepository.search(query.trim(), pageable)
                : manufacturerRepository.findByDeletedFalse(pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManufacturerDto> findAllActive() {
        return manufacturerRepository.findByDeletedFalse(Sort.by("name")).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManufacturerDto> findDeleted(Pageable pageable) {
        return manufacturerRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ManufacturerDto findById(Long id) {
        return toDto(activeManufacturer(id));
    }

    @Override
    public ManufacturerDto create(ManufacturerDto dto, Actor actor) {
        actor.requireStaff();
        String name = dto.getName().trim();
        assertNameAvailable(name, null);
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(name);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        auditService.record(actor, AuditAction.CREATED, ENTITY, saved.getId());
        log.debug("Manufacturer {} created", saved.getId());
        return toDto(saved);
    }

    @Override
    public ManufacturerDto update(Long id, ManufacturerDto dto, Actor actor) {
        actor.requireStaff();
        Manufacturer manufacturer = activeManufacturer(id);
        String name = dto.getName().trim();
        assertNameAvailable(name, id);
        manufacturer.setName(name);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        auditService.record(actor, AuditAction.UPDATED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public void delete(Long id, Actor actor) {
        actor.requireStaff();
        Manufacturer manufacturer = activeManufacturer(id);
        if (applianceRepository.existsByManufacturerIdAndDeletedFalse(id)) {
            throw new OperationNotAllowedException("error.manufacturer.inUse");
        }
        manufacturerRepository.delete(manufacturer);
        auditService.record(actor, AuditAction.DELETED, ENTITY, id);
    }

    @Override
    public void restore(Long id, Actor actor) {
        actor.requireStaff();
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        if (!manufacturer.isDeleted()) {
            return;
        }
        assertNameAvailable(manufacturer.getName(), id);
        manufacturerRepository.restore(id);
        auditService.record(actor, AuditAction.RESTORED, ENTITY, id);
    }

    private Manufacturer activeManufacturer(Long id) {
        return manufacturerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private void assertNameAvailable(String name, Long ownId) {
        manufacturerRepository.findByNameIgnoreCaseAndDeletedFalse(name)
                .filter(found -> !Objects.equals(found.getId(), ownId))
                .ifPresent(found -> {
                    throw new OperationNotAllowedException("error.manufacturer.nameTaken");
                });
    }

    private ManufacturerDto toDto(Manufacturer manufacturer) {
        return modelMapper.map(manufacturer, ManufacturerDto.class);
    }
}
