package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceFilterDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.ApplianceTranslation;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceTranslationRepository;
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

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Loggable
@Service
@Transactional
@RequiredArgsConstructor
public class ApplianceServiceImpl implements ApplianceService {

    private static final String ENTITY = "Appliance";

    private final ApplianceRepository applianceRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ApplianceTranslationRepository translationRepository;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceDto> search(ApplianceFilterDto filter, Pageable pageable) {
        ApplianceFilterDto criteria = filter == null ? new ApplianceFilterDto() : filter;
        Page<ApplianceDto> page = applianceRepository
                .findAll(ApplianceSpecifications.filter(criteria.getQuery(), criteria.getCategory(),
                        criteria.getManufacturerId(), criteria.getPowerType()), pageable)
                .map(this::toDto);
        return withTranslations(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceDto> findDeleted(Pageable pageable) {
        return withTranslations(applianceRepository.findByDeletedTrue(pageable).map(this::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplianceDto findById(Long id) {
        ApplianceDto dto = toDto(activeAppliance(id));
        applyTranslation(dto, translationRepository.findByApplianceIdAndLanguage(id, ApplianceTranslation.UK).orElse(null));
        return dto;
    }

    @Override
    public ApplianceDto create(ApplianceDto dto, Actor actor) {
        actor.requireStaff();
        Appliance appliance = modelMapper.map(dto, Appliance.class);
        appliance.setId(null);
        appliance.setManufacturer(activeManufacturer(dto.getManufacturerId()));
        Appliance saved = applianceRepository.save(appliance);
        saveTranslation(saved.getId(), dto);
        auditService.record(actor, AuditAction.CREATED, ENTITY, saved.getId());
        log.debug("Appliance {} created", saved.getId());
        return toDto(saved, dto);
    }

    @Override
    public ApplianceDto update(Long id, ApplianceDto dto, Actor actor) {
        actor.requireStaff();
        Appliance appliance = activeAppliance(id);
        dto.setId(id);
        modelMapper.map(dto, appliance);
        appliance.setManufacturer(activeManufacturer(dto.getManufacturerId()));
        Appliance saved = applianceRepository.save(appliance);
        saveTranslation(id, dto);
        auditService.record(actor, AuditAction.UPDATED, ENTITY, id);
        return toDto(saved, dto);
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

    private ApplianceDto toDto(Appliance appliance, ApplianceDto source) {
        ApplianceDto dto = toDto(appliance);
        dto.setNameUk(source.getNameUk());
        dto.setCharacteristicUk(source.getCharacteristicUk());
        dto.setDescriptionUk(source.getDescriptionUk());
        return dto;
    }

    private Page<ApplianceDto> withTranslations(Page<ApplianceDto> page) {
        if (page.isEmpty()) {
            return page;
        }
        Map<Long, ApplianceTranslation> byAppliance = translationRepository
                .findByApplianceIdInAndLanguage(page.getContent().stream().map(ApplianceDto::getId).toList(), ApplianceTranslation.UK)
                .stream().collect(Collectors.toMap(ApplianceTranslation::getApplianceId, Function.identity()));
        page.getContent().forEach(dto -> applyTranslation(dto, byAppliance.get(dto.getId())));
        return page;
    }

    private void applyTranslation(ApplianceDto dto, ApplianceTranslation translation) {
        if (translation != null) {
            dto.setNameUk(translation.getName());
            dto.setCharacteristicUk(translation.getCharacteristic());
            dto.setDescriptionUk(translation.getDescription());
        }
    }

    private void saveTranslation(Long applianceId, ApplianceDto dto) {
        Optional<ApplianceTranslation> existing = translationRepository.findByApplianceIdAndLanguage(applianceId, ApplianceTranslation.UK);
        boolean empty = isBlank(dto.getNameUk()) && isBlank(dto.getCharacteristicUk()) && isBlank(dto.getDescriptionUk());
        if (empty) {
            existing.ifPresent(translationRepository::delete);
            return;
        }
        ApplianceTranslation translation = existing.orElseGet(() -> ApplianceTranslation.builder()
                .applianceId(applianceId).language(ApplianceTranslation.UK).build());
        translation.setName(blankToNull(dto.getNameUk()));
        translation.setCharacteristic(blankToNull(dto.getCharacteristicUk()));
        translation.setDescription(blankToNull(dto.getDescriptionUk()));
        translationRepository.save(translation);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
