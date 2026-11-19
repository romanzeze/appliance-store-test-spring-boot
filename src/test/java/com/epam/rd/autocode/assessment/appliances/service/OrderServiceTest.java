package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceInOrderRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Actor CLIENT = new Actor(7L, Role.USER);
    private static final Actor STAFF = new Actor(50L, Role.EMPLOYEE);

    @Mock
    private OrdersRepository ordersRepository;
    @Mock
    private ApplianceInOrderRepository orderRowRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ApplianceRepository applianceRepository;
    @Mock
    private AuditService auditService;

    private OrderServiceImpl service;
    private Appliance washer;
    private Appliance dryer;
    private Client client;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(ordersRepository, orderRowRepository, clientRepository, employeeRepository,
                applianceRepository, ServiceTestSupport.mapper(), auditService);
        Manufacturer bosch = new Manufacturer(3L, "Bosch");
        washer = new Appliance(10L, "Washer", Category.BIG, "S6", bosch, PowerType.AC220, null, null, 2300,
                new BigDecimal("649.00"));
        dryer = new Appliance(11L, "Dryer", Category.SMALL, "D1", bosch, PowerType.AC220, null, null, 2200,
                new BigDecimal("39.90"));
        client = new Client(7L, "Luka Doncic", "luka@example.com", "hash", "5326-7705");
    }

    private OrderDto request(OrderRowDto... rows) {
        OrderDto dto = new OrderDto();
        dto.setRows(List.of(rows));
        return dto;
    }

    private OrderRowDto row(Long applianceId, Long number) {
        return OrderRowDto.builder().applianceId(applianceId).number(number).build();
    }

    private Orders order(Long id, boolean approved) {
        Orders order = new Orders(id, approved, client, null, new LinkedHashSet<>());
        OrderRow existing = new OrderRow(100L, washer, new BigDecimal("649.00"), 1L);
        order.getOrderRowSet().add(existing);
        return order;
    }

    @Test
    void clientCreatesOrderWithServerSideAmounts() {
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(client));
        when(applianceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(washer));
        when(applianceRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(dryer));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(call -> {
            Orders saved = call.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        OrderDto dto = request(row(10L, 2L), row(11L, 1L), row(10L, 1L));
        dto.setClientId(999L);

        OrderDto result = service.create(dto, CLIENT);

        assertEquals(20L, result.getId());
        assertEquals(7L, result.getClientId());
        assertFalse(result.getApproved());
        assertEquals(2, result.getRows().size());
        assertEquals(new BigDecimal("1986.90"), result.getTotalAmount());
        verify(auditService).record(CLIENT, AuditAction.CREATED, "Order", 20L);
    }

    @Test
    void staffCreatesOrderForGivenClient() {
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(client));
        when(applianceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(washer));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(call -> call.getArgument(0));
        OrderDto dto = request(row(10L, 1L));
        dto.setClientId(7L);

        OrderDto result = service.create(dto, STAFF);

        assertEquals(7L, result.getClientId());
    }

    @Test
    void createFailsForUnknownAppliance() {
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(client));
        when(applianceRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(request(row(99L, 1L)), CLIENT));
        verify(ordersRepository, never()).save(any());
    }

    @Test
    void staffMustProvideClientWhenCreating() {
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(row(10L, 1L)), STAFF));
    }

    @Test
    void clientSeesOnlyOwnOrders() {
        when(ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(20L, 7L)).thenReturn(Optional.of(order(20L, false)));
        when(ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(20L, 8L)).thenReturn(Optional.empty());

        assertEquals(20L, service.findById(20L, CLIENT).getId());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(20L, new Actor(8L, Role.USER)));
    }

    @Test
    void staffSeesAnyOrder() {
        when(ordersRepository.findWithRowsByIdAndDeletedFalse(20L)).thenReturn(Optional.of(order(20L, false)));

        assertEquals(20L, service.findById(20L, STAFF).getId());
    }

    @Test
    void clientCannotListAllOrdersOrOthersOrders() {
        assertThrows(AccessDeniedException.class, () -> service.findAll(null, PageRequest.of(0, 5), CLIENT));
        assertThrows(AccessDeniedException.class, () -> service.findByClient(8L, PageRequest.of(0, 5), CLIENT));
    }

    @Test
    void updateReplacesRowsAndDeletesPreviousOnes() {
        Orders existing = order(20L, false);
        OrderRow previous = existing.getOrderRowSet().iterator().next();
        when(ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(20L, 7L)).thenReturn(Optional.of(existing));
        when(applianceRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(dryer));
        when(ordersRepository.save(existing)).thenReturn(existing);

        OrderDto result = service.update(20L, request(row(11L, 3L)), CLIENT);

        assertEquals(1, result.getRows().size());
        assertEquals(new BigDecimal("119.70"), result.getTotalAmount());
        ArgumentCaptor<Iterable<OrderRow>> deleted = ArgumentCaptor.forClass(Iterable.class);
        verify(orderRowRepository).deleteAll(deleted.capture());
        assertEquals(Set.of(previous), new HashSet<>(toList(deleted.getValue())));
        verify(auditService).record(CLIENT, AuditAction.UPDATED, "Order", 20L);
    }

    private List<OrderRow> toList(Iterable<OrderRow> rows) {
        List<OrderRow> list = new java.util.ArrayList<>();
        rows.forEach(list::add);
        return list;
    }

    @Test
    void approvedOrderCannotBeUpdated() {
        when(ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(20L, 7L)).thenReturn(Optional.of(order(20L, true)));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.update(20L, request(row(10L, 1L)), CLIENT));

        assertEquals("error.order.approved", exception.getMessageKey());
        verify(orderRowRepository, never()).deleteAll(any());
    }

    @Test
    void staffApprovesOrder() {
        Orders existing = order(20L, false);
        Employee employee = new Employee(50L, "Michael Jordan", "mj@example.com", "hash", "sales");
        when(ordersRepository.findWithRowsByIdAndDeletedFalse(20L)).thenReturn(Optional.of(existing));
        when(employeeRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(employee));
        when(ordersRepository.save(existing)).thenReturn(existing);

        OrderDto result = service.approve(20L, STAFF);

        assertTrue(result.getApproved());
        assertEquals("Michael Jordan", result.getEmployeeName());
        verify(auditService).record(STAFF, AuditAction.APPROVED, "Order", 20L);
    }

    @Test
    void alreadyApprovedOrderCannotBeApprovedAgain() {
        when(ordersRepository.findWithRowsByIdAndDeletedFalse(20L)).thenReturn(Optional.of(order(20L, true)));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.approve(20L, STAFF));

        assertEquals("error.order.alreadyApproved", exception.getMessageKey());
    }

    @Test
    void clientCannotApprove() {
        assertThrows(AccessDeniedException.class, () -> service.approve(20L, CLIENT));
    }

    @Test
    void clientDeletesOwnUnapprovedOrder() {
        Orders existing = order(20L, false);
        when(ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(20L, 7L)).thenReturn(Optional.of(existing));

        service.delete(20L, CLIENT);

        verify(ordersRepository).delete(existing);
        verify(auditService).record(CLIENT, AuditAction.DELETED, "Order", 20L);
    }

    @Test
    void clientCannotDeleteApprovedOrderButStaffCan() {
        Orders approved = order(20L, true);
        when(ordersRepository.findWithRowsByIdAndClientIdAndDeletedFalse(20L, 7L)).thenReturn(Optional.of(approved));
        when(ordersRepository.findWithRowsByIdAndDeletedFalse(20L)).thenReturn(Optional.of(approved));

        assertThrows(OperationNotAllowedException.class, () -> service.delete(20L, CLIENT));
        service.delete(20L, STAFF);

        verify(ordersRepository).delete(approved);
    }

    @Test
    void restoreBringsOrderBack() {
        Orders deleted = order(20L, false);
        deleted.setDeleted(true);
        when(ordersRepository.findById(20L)).thenReturn(Optional.of(deleted));

        service.restore(20L, STAFF);

        verify(ordersRepository).restore(20L);
        verify(auditService).record(STAFF, AuditAction.RESTORED, "Order", 20L);
    }
}
