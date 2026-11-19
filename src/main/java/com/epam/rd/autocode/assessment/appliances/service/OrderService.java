package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    Page<OrderDto> findAll(Boolean approved, Pageable pageable, Actor actor);

    Page<OrderDto> findByClient(Long clientId, Pageable pageable, Actor actor);

    Page<OrderDto> findDeleted(Pageable pageable, Actor actor);

    OrderDto findById(Long id, Actor actor);

    OrderDto create(OrderDto dto, Actor actor);

    OrderDto update(Long id, OrderDto dto, Actor actor);

    OrderDto approve(Long id, Actor actor);

    void delete(Long id, Actor actor);

    void restore(Long id, Actor actor);
}
