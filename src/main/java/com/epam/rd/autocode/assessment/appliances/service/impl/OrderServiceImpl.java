package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceInOrderRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Loggable
@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ENTITY = "Order";

    private final OrdersRepository ordersRepository;
    private final ApplianceInOrderRepository orderRowRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplianceRepository applianceRepository;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> findAll(Boolean approved, Pageable pageable, Actor actor) {
        actor.requireStaff();
        Page<Orders> page = approved == null
                ? ordersRepository.findByDeletedFalse(pageable)
                : ordersRepository.findByApprovedAndDeletedFalse(approved, pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> findByClient(Long clientId, Pageable pageable, Actor actor) {
        if (!actor.isStaff() && !actor.is(Role.USER, clientId)) {
            throw new AccessDeniedException("Orders of another client are not available");
        }
        return ordersRepository.findByClientIdAndDeletedFalse(clientId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> findDeleted(Pageable pageable, Actor actor) {
        actor.requireStaff();
        return ordersRepository.findByDeletedTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto findById(Long id, Actor actor) {
        return toDto(visibleOrder(id, actor));
    }

    @Override
    public OrderDto create(OrderDto dto, Actor actor) {
        Long clientId = actor.isStaff() ? dto.getClientId() : actor.id();
        Client client = activeClient(clientId);
        Orders order = new Orders();
        order.setClient(client);
        order.setApproved(false);
        order.setOrderRowSet(buildRows(dto.getRows()));
        Orders saved = ordersRepository.save(order);
        auditService.record(actor, AuditAction.CREATED, ENTITY, saved.getId());
        log.debug("Order {} created for client {}", saved.getId(), clientId);
        return toDto(saved);
    }

    @Override
    public OrderDto update(Long id, OrderDto dto, Actor actor) {
        Orders order = visibleOrder(id, actor);
        assertNotApproved(order);
        List<OrderRow> previousRows = new ArrayList<>(order.getOrderRowSet());
        Set<OrderRow> newRows = buildRows(dto.getRows());
        order.getOrderRowSet().clear();
        order.getOrderRowSet().addAll(newRows);
        Orders saved = ordersRepository.save(order);
        orderRowRepository.deleteAll(previousRows);
        auditService.record(actor, AuditAction.UPDATED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public OrderDto approve(Long id, Actor actor) {
        actor.requireStaff();
        Orders order = visibleOrder(id, actor);
        if (Boolean.TRUE.equals(order.getApproved())) {
            throw new OperationNotAllowedException("error.order.alreadyApproved");
        }
        Employee employee = employeeRepository.findByIdAndDeletedFalse(actor.id())
                .orElseThrow(() -> new OperationNotAllowedException("error.order.approverNotEmployee"));
        order.setApproved(true);
        order.setEmployee(employee);
        Orders saved = ordersRepository.save(order);
        auditService.record(actor, AuditAction.APPROVED, ENTITY, id);
        return toDto(saved);
    }

    @Override
    public void delete(Long id, Actor actor) {
        Orders order = visibleOrder(id, actor);
        if (!actor.isStaff()) {
            assertNotApproved(order);
        }
        ordersRepository.delete(order);
        auditService.record(actor, AuditAction.DELETED, ENTITY, id);
    }

    @Override
    public void restore(Long id, Actor actor) {
        actor.requireStaff();
        Orders order = ordersRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        if (!order.isDeleted()) {
            return;
        }
        ordersRepository.restore(id);
        auditService.record(actor, AuditAction.RESTORED, ENTITY, id);
    }

    private Orders visibleOrder(Long id, Actor actor) {
        return (actor.isStaff()
                ? ordersRepository.findWithRowsByIdAndDeletedFalse(id)
                : ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(id, actor.id()))
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private Client activeClient(Long clientId) {
        if (clientId == null) {
            throw new ResourceNotFoundException("Client", null);
        }
        return clientRepository.findByIdAndDeletedFalse(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
    }

    private void assertNotApproved(Orders order) {
        if (Boolean.TRUE.equals(order.getApproved())) {
            throw new OperationNotAllowedException("error.order.approved");
        }
    }

    private Set<OrderRow> buildRows(List<OrderRowDto> rowDtos) {
        Map<Long, Long> quantities = new LinkedHashMap<>();
        rowDtos.forEach(row -> quantities.merge(row.getApplianceId(), row.getNumber(), Long::sum));
        Set<OrderRow> rows = new LinkedHashSet<>();
        quantities.forEach((applianceId, number) -> {
            Appliance appliance = applianceRepository.findByIdAndDeletedFalse(applianceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appliance", applianceId));
            OrderRow row = new OrderRow();
            row.setAppliance(appliance);
            row.setNumber(number);
            row.setAmount(appliance.getPrice().multiply(BigDecimal.valueOf(number)));
            rows.add(row);
        });
        return rows;
    }

    private OrderDto toDto(Orders order) {
        OrderDto dto = modelMapper.map(order, OrderDto.class);
        dto.getRows().sort(Comparator.comparing(OrderRowDto::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return dto;
    }
}
