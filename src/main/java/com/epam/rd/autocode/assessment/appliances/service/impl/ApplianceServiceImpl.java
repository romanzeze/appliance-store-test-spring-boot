package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceFilterDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceSpecifications;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ApplianceServiceImpl implements ApplianceService {

    private static final String ENTITY = "Appliance";

    private final ApplianceRepository applianceRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceDto> search(ApplianceFilterDto filter, Pageable pageable) {
        ApplianceFilterDto criteria = filter == null ? new ApplianceFilterDto() : filter;
        return applianceRepository
                .findAll(ApplianceSpecifications.filter(criteria.getQuery(), criteria.getCategory(),
                        criteria.getManufacturerId(), criteria.getPowerType()), pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceDto> findDeleted(Pageable pageable) {
        return applianceRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplianceDto findById(Long id) {
        return toDto(activeAppliance(id));
    }

    @Override
    public ApplianceDto create(ApplianceDto dto, Actor actor) {
        actor.requireStaff();
        Appliance appliance = modelMapper.map(dto, Appliance.class);
        appliance.setId(null);
        appliance.setManufacturer(activeManufacturer(dto.getManufacturerId()));
        Appliance saved = applianceRepository.save(appliance);
        auditService.record(actor, AuditAction.CREATED, ENTITY, saved.getId());
        log.debug("Appliance {} created", saved.getId());
        return toDto(saved);
    }

    @Override
    public ApplianceDto update(Long id, ApplianceDto dto, Actor actor) {
        actor.requireStaff();
        Appliance appliance = activeAppliance(id);
        dto.setId(id);
        modelMapper.map(dto, appliance);
        appliance.setManufacturer(activeManufacturer(dto.getManufacturerId()));
        Appliance saved = applianceRepository.save(appliance);
        auditService.record(actor, AuditAction.UPDATED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public void delete(Long id, Actor actor) {
        actor.requireStaff();
        applianceRepository.delete(activeAppliance(id));
        auditService.record(actor, AuditAction.DELETED, ENTITY, id);
    }

    @Override
    public void restore(Long id, Actor actor) {
        actor.requireStaff();
        Appliance appliance = applianceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        if (!appliance.isDeleted()) {
            return;
        }
        if (appliance.getManufacturer().isDeleted()) {
            throw new OperationNotAllowedException("error.appliance.manufacturerDeleted");
        }
        applianceRepository.restore(id);
        auditService.record(actor, AuditAction.RESTORED, ENTITY, id);
    }

    private Appliance activeAppliance(Long id) {
        return applianceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private Manufacturer activeManufacturer(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Manufacturer", null);
        }
        return manufacturerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));
    }

    private ApplianceDto toDto(Appliance appliance) {
        return modelMapper.map(appliance, ApplianceDto.class);
    }
}
