package com.epam.rd.autocode.assessment.appliances.config;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Set;

@Configuration
public class MainConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        mapper.typeMap(Appliance.class, ApplianceDto.class).addMappings(m -> {
            m.map(source -> source.getManufacturer().getId(), ApplianceDto::setManufacturerId);
            m.map(source -> source.getManufacturer().getName(), ApplianceDto::setManufacturerName);
        });

        mapper.typeMap(ApplianceDto.class, Appliance.class)
                .addMappings(m -> m.skip(Appliance::setManufacturer));

        mapper.typeMap(ClientDto.class, Client.class).addMappings(m -> {
            m.skip(Client::setRole);
            m.skip(Client::setBlocked);
            m.skip(Client::setDeleted);
            m.skip(Client::setMustChangePassword);
            m.skip(Client::setPasswordExpiresAt);
            m.skip(Client::setPassword);
        });

        mapper.typeMap(EmployeeDto.class, Employee.class).addMappings(m -> {
            m.skip(Employee::setRole);
            m.skip(Employee::setBlocked);
            m.skip(Employee::setDeleted);
            m.skip(Employee::setMustChangePassword);
            m.skip(Employee::setPasswordExpiresAt);
            m.skip(Employee::setPassword);
        });

        mapper.typeMap(RegistrationDto.class, Client.class).addMappings(m -> {
            m.skip(Client::setId);
            m.skip(Client::setPassword);
            m.skip(Client::setRole);
            m.skip(Client::setBlocked);
            m.skip(Client::setDeleted);
            m.skip(Client::setMustChangePassword);
            m.skip(Client::setPasswordExpiresAt);
        });

        Converter<Set<OrderRow>, BigDecimal> total = context -> context.getSource() == null
                ? BigDecimal.ZERO
                : context.getSource().stream()
                .map(OrderRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        mapper.typeMap(Orders.class, OrderDto.class).addMappings(m -> {
            m.map(source -> source.getClient().getId(), OrderDto::setClientId);
            m.map(source -> source.getClient().getName(), OrderDto::setClientName);
            m.map(source -> source.getEmployee().getId(), OrderDto::setEmployeeId);
            m.map(source -> source.getEmployee().getName(), OrderDto::setEmployeeName);
            m.map(Orders::getOrderRowSet, OrderDto::setRows);
            m.using(total).map(Orders::getOrderRowSet, OrderDto::setTotalAmount);
        });

        mapper.typeMap(OrderRow.class, OrderRowDto.class).addMappings(m -> {
            m.map(source -> source.getAppliance().getId(), OrderRowDto::setApplianceId);
            m.map(source -> source.getAppliance().getName(), OrderRowDto::setApplianceName);
        });

        return mapper;
    }
}
