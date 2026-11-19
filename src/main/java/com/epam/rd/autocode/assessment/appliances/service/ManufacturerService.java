package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ManufacturerService {

    Page<ManufacturerDto> findAll(String query, Pageable pageable);

    List<ManufacturerDto> findAllActive();

    Page<ManufacturerDto> findDeleted(Pageable pageable);

    ManufacturerDto findById(Long id);

    ManufacturerDto create(ManufacturerDto dto, Actor actor);

    ManufacturerDto update(Long id, ManufacturerDto dto, Actor actor);

    void delete(Long id, Actor actor);

    void restore(Long id, Actor actor);
}
