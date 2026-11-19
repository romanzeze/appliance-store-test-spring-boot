package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplianceService {

    Page<ApplianceDto> search(ApplianceFilterDto filter, Pageable pageable);

    Page<ApplianceDto> findDeleted(Pageable pageable);

    ApplianceDto findById(Long id);

    ApplianceDto create(ApplianceDto dto, Actor actor);

    ApplianceDto update(Long id, ApplianceDto dto, Actor actor);

    void delete(Long id, Actor actor);

    void restore(Long id, Actor actor);
}
