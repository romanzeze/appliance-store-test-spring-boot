package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    Page<ClientDto> findAll(String query, Pageable pageable);

    Page<ClientDto> findDeleted(Pageable pageable);

    ClientDto findById(Long id);

    ClientDto register(RegistrationDto dto);

    ClientDto update(Long id, ClientDto dto, Actor actor);

    void changePassword(Long id, ChangePasswordDto dto, Actor actor);

    void block(Long id, Actor actor);

    void unblock(Long id, Actor actor);

    void deactivate(Long id, Actor actor);

    void restore(Long id, Actor actor);

    void eraseAllData(Long id, Actor actor);
}
