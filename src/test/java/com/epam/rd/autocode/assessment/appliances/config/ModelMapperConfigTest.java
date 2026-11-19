package com.epam.rd.autocode.assessment.appliances.config;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelMapperConfigTest {

    private ModelMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MainConfig().modelMapper();
    }

    private Appliance appliance() {
        Manufacturer manufacturer = new Manufacturer(3L, "Bosch");
        return new Appliance(10L, "Washing machine", Category.BIG, "Series 6", manufacturer,
                PowerType.AC220, "9 kg", "Front loader", 2300, new BigDecimal("649.00"));
    }

    @Test
    void mapsApplianceToDtoWithManufacturer() {
        ApplianceDto dto = mapper.map(appliance(), ApplianceDto.class);

        assertEquals(10L, dto.getId());
        assertEquals(3L, dto.getManufacturerId());
        assertEquals("Bosch", dto.getManufacturerName());
        assertEquals(Category.BIG, dto.getCategory());
        assertEquals(new BigDecimal("649.00"), dto.getPrice());
    }

    @Test
    void mappingDtoToApplianceLeavesManufacturerForTheService() {
        ApplianceDto dto = mapper.map(appliance(), ApplianceDto.class);

        Appliance result = mapper.map(dto, Appliance.class);

        assertEquals("Washing machine", result.getName());
        assertNull(result.getManufacturer());
    }

    @Test
    void clientDtoCannotChangeSecurityFields() {
        Client client = new Client(1L, "LeBron James", "lebron@example.com", "hash", "5326-2301");
        client.setBlocked(false);
        ClientDto dto = mapper.map(client, ClientDto.class);
        dto.setBlocked(true);
        dto.setName("Changed");

        mapper.map(dto, client);

        assertEquals("Changed", client.getName());
        assertFalse(client.isBlocked());
        assertEquals(Role.USER, client.getRole());
        assertEquals("hash", client.getPassword());
    }

    @Test
    void employeeDtoCannotChangeRoleOrPassword() {
        Employee employee = new Employee(2L, "Michael Jordan", "mj@example.com", "hash", "sales");
        EmployeeDto dto = mapper.map(employee, EmployeeDto.class);
        dto.setRole(Role.ADMIN);
        dto.setDepartment("security");

        mapper.map(dto, employee);

        assertEquals("security", employee.getDepartment());
        assertEquals(Role.EMPLOYEE, employee.getRole());
        assertEquals("hash", employee.getPassword());
    }

    @Test
    void registrationDoesNotCopyPlainPassword() {
        RegistrationDto dto = new RegistrationDto("Luka Doncic", "luka@example.com", "Plain-1!", "Plain-1!", "5326-7705");

        Client client = mapper.map(dto, Client.class);

        assertEquals("Luka Doncic", client.getName());
        assertEquals("5326-7705", client.getCard());
        assertNull(client.getPassword());
        assertEquals(Role.USER, client.getRole());
    }

    @Test
    void mapsOrderWithRowsAndTotal() {
        Client client = new Client(1L, "LeBron James", "lebron@example.com", "hash", "5326-2301");
        Employee employee = new Employee(2L, "Michael Jordan", "mj@example.com", "hash", "sales");
        Set<OrderRow> rows = new LinkedHashSet<>();
        rows.add(new OrderRow(1L, appliance(), new BigDecimal("1298.00"), 2L));
        rows.add(new OrderRow(2L, appliance(), new BigDecimal("649.00"), 1L));
        Orders order = new Orders(5L, true, client, employee, rows);

        OrderDto dto = mapper.map(order, OrderDto.class);

        assertEquals(5L, dto.getId());
        assertEquals(1L, dto.getClientId());
        assertEquals("LeBron James", dto.getClientName());
        assertEquals("Michael Jordan", dto.getEmployeeName());
        assertEquals(2, dto.getRows().size());
        assertEquals("Washing machine", dto.getRows().get(0).getApplianceName());
        assertEquals(new BigDecimal("1947.00"), dto.getTotalAmount());
    }

    @Test
    void mapsOrderOfErasedClient() {
        Orders order = new Orders(6L, false, null, null, new LinkedHashSet<>());

        OrderDto dto = mapper.map(order, OrderDto.class);

        assertNull(dto.getClientId());
        assertNull(dto.getClientName());
        assertEquals(BigDecimal.ZERO, dto.getTotalAmount());
    }
}
