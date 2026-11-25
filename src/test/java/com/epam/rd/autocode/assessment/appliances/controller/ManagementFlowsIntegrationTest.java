package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.security.AppUserDetails;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagementFlowsIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ManufacturerService manufacturerService;
    @Autowired
    private ApplianceService applianceService;
    @Autowired
    private OrderService orderService;

    private Client client;
    private Employee employee;
    private Employee admin;
    private Employee target;
    private ManufacturerDto manufacturer;
    private ApplianceDto appliance;
    private OrderDto order;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(new Client(null, "Magic", "magic@flow.test", passwordEncoder.encode(PASSWORD), "1234 5678"));
        employee = employeeRepository.save(staff("Pippen", "pippen@flow.test", Role.EMPLOYEE));
        admin = employeeRepository.save(staff("Kobe", "kobe@flow.test", Role.ADMIN));
        target = employeeRepository.save(staff("Target", "target@flow.test", Role.EMPLOYEE));
        Actor actor = new Actor(admin.getId(), Role.ADMIN);
        manufacturer = manufacturerService.create(ManufacturerDto.builder().name("Flow Corp").build(), actor);
        appliance = applianceService.create(ApplianceDto.builder()
                .name("Flow Oven").category(Category.BIG).model("FO-1").manufacturerId(manufacturer.getId())
                .powerType(PowerType.AC220).power(150).price(new BigDecimal("99.90")).build(), actor);
        order = orderService.create(OrderDto.builder()
                .rows(new java.util.ArrayList<>(List.of(OrderRowDto.builder().applianceId(appliance.getId()).number(1L).build())))
                .build(), new Actor(client.getId(), Role.USER));
    }

    private Employee staff(String name, String email, Role role) {
        Employee created = new Employee(null, name, email, passwordEncoder.encode(PASSWORD), "Sales");
        created.setRole(role);
        return created;
    }

    private AppUserDetails asClient() {
        return ControllerTestSupport.details(client);
    }

    private AppUserDetails asEmployee() {
        return ControllerTestSupport.details(employee);
    }

    private AppUserDetails asAdmin() {
        return ControllerTestSupport.details(admin);
    }

    private MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder builder, AppUserDetails who) {
        return builder.with(user(who)).with(csrf());
    }

    @Test
    void adminCreatesEmployeeAndSeesTemporaryPassword() throws Exception {
        mockMvc.perform(get("/employees/add").with(user(asAdmin())))
                .andExpect(status().isOk()).andExpect(view().name("employee/form"));
        mockMvc.perform(form(post("/employees/add"), asAdmin())
                        .param("name", "Fresh").param("email", "fresh@flow.test").param("department", "Ops").param("role", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/credentials"))
                .andExpect(model().attributeExists("temporaryPassword"));
    }

    @Test
    void employeeCreationRejectsInvalidAndDuplicateData() throws Exception {
        mockMvc.perform(form(post("/employees/add"), asAdmin()).param("name", "").param("email", "bad"))
                .andExpect(status().isOk()).andExpect(view().name("employee/form"));
        mockMvc.perform(form(post("/employees/add"), asAdmin())
                        .param("name", "Dup").param("email", "target@flow.test").param("department", "Ops").param("role", "EMPLOYEE"))
                .andExpect(status().isOk()).andExpect(view().name("employee/form"));
    }

    @Test
    void adminEditsAndManagesEmployee() throws Exception {
        String base = "/employees/" + target.getId();
        mockMvc.perform(get(base + "/edit").with(user(asAdmin()))).andExpect(status().isOk()).andExpect(view().name("employee/form"));
        mockMvc.perform(form(post(base + "/edit"), asAdmin())
                        .param("name", "Renamed").param("email", "target@flow.test").param("department", "Ops").param("role", "EMPLOYEE"))
                .andExpect(redirectedUrl("/employees"));
        mockMvc.perform(form(post(base + "/edit"), asAdmin()).param("name", "").param("email", "x"))
                .andExpect(status().isOk()).andExpect(view().name("employee/form"));
        mockMvc.perform(form(post(base + "/edit"), asAdmin())
                        .param("name", "Dup").param("email", "pippen@flow.test").param("department", "Ops").param("role", "EMPLOYEE"))
                .andExpect(status().isOk()).andExpect(view().name("employee/form"));
        mockMvc.perform(form(post(base + "/role"), asAdmin()).param("role", "ADMIN")).andExpect(redirectedUrl("/employees"));
        mockMvc.perform(form(post(base + "/reset-password"), asAdmin()))
                .andExpect(status().isOk()).andExpect(view().name("employee/credentials"));
        mockMvc.perform(form(post(base + "/block"), asAdmin())).andExpect(redirectedUrl("/employees"));
        mockMvc.perform(form(post(base + "/unblock"), asAdmin())).andExpect(redirectedUrl("/employees"));
        mockMvc.perform(form(post(base + "/delete"), asAdmin())).andExpect(redirectedUrl("/employees"));
        mockMvc.perform(get("/employees/deleted").with(user(asAdmin())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("target@flow.test")));
        mockMvc.perform(form(post(base + "/restore"), asAdmin())).andExpect(redirectedUrl("/employees/deleted"));
    }

    @Test
    void employeeManagesManufacturers() throws Exception {
        String base = "/manufacturers/" + manufacturer.getId();
        mockMvc.perform(get("/manufacturers/add").with(user(asEmployee()))).andExpect(status().isOk());
        mockMvc.perform(get(base + "/edit").with(user(asEmployee()))).andExpect(status().isOk()).andExpect(view().name("manufacturer/form"));
        mockMvc.perform(form(post(base + "/edit"), asEmployee()).param("name", "Flow Renamed")).andExpect(redirectedUrl("/manufacturers"));
        mockMvc.perform(form(post(base + "/edit"), asEmployee()).param("name", " "))
                .andExpect(status().isOk()).andExpect(view().name("manufacturer/form"));
        mockMvc.perform(form(post(base + "/delete"), asEmployee())).andExpect(status().isConflict());
        ManufacturerDto free = manufacturerService.create(ManufacturerDto.builder().name("Free Corp").build(), new Actor(admin.getId(), Role.ADMIN));
        String freeBase = "/manufacturers/" + free.getId();
        mockMvc.perform(form(post(freeBase + "/delete"), asEmployee())).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/manufacturers/deleted").with(user(asAdmin())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Free Corp")));
        mockMvc.perform(form(post(freeBase + "/restore"), asAdmin())).andExpect(status().is3xxRedirection());
    }

    @Test
    void employeeManagesAppliances() throws Exception {
        String base = "/appliances/" + appliance.getId();
        mockMvc.perform(get("/appliances/add").with(user(asEmployee()))).andExpect(status().isOk()).andExpect(view().name("appliance/form"));
        mockMvc.perform(form(post("/appliances/add"), asEmployee())
                        .param("name", "Flow Mixer").param("category", "SMALL").param("model", "FM-1")
                        .param("manufacturerId", manufacturer.getId().toString()).param("powerType", "AC110")
                        .param("power", "300").param("price", "19.90"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(form(post("/appliances/add"), asEmployee()).param("name", ""))
                .andExpect(status().isOk()).andExpect(view().name("appliance/form"));
        mockMvc.perform(get(base + "/edit").with(user(asEmployee()))).andExpect(status().isOk()).andExpect(view().name("appliance/form"));
        mockMvc.perform(form(post(base + "/edit"), asEmployee())
                        .param("name", "Flow Oven 2").param("category", "BIG").param("model", "FO-2")
                        .param("manufacturerId", manufacturer.getId().toString()).param("powerType", "AC220")
                        .param("power", "200").param("price", "120.00"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(form(post(base + "/edit"), asEmployee()).param("name", ""))
                .andExpect(status().isOk()).andExpect(view().name("appliance/form"));
        mockMvc.perform(form(post(base + "/delete"), asEmployee())).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/appliances/deleted").with(user(asAdmin())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Flow Oven 2")));
        mockMvc.perform(form(post(base + "/restore"), asAdmin())).andExpect(status().is3xxRedirection());
    }

    @Test
    void employeeManagesClients() throws Exception {
        String base = "/clients/" + client.getId();
        mockMvc.perform(get(base + "/edit").with(user(asEmployee()))).andExpect(status().isOk()).andExpect(view().name("client/form"));
        mockMvc.perform(form(post(base + "/edit"), asEmployee()).param("name", "").param("email", "bad"))
                .andExpect(status().isOk()).andExpect(view().name("client/form"));
        mockMvc.perform(form(post(base + "/delete"), asEmployee())).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/clients/deleted").with(user(asAdmin())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("magic@flow.test")));
        mockMvc.perform(form(post(base + "/restore"), asAdmin())).andExpect(status().is3xxRedirection());
    }

    @Test
    void ordersCanBeViewedEditedApprovedDeletedAndRestored() throws Exception {
        String base = "/orders/" + order.getId();
        mockMvc.perform(get(base + "/edit").with(user(asClient()))).andExpect(status().isOk()).andExpect(view().name("order/form"));
        mockMvc.perform(form(post(base + "/edit"), asClient())
                        .param("rows[0].applianceId", appliance.getId().toString()).param("rows[0].number", "3"))
                .andExpect(redirectedUrl(base));
        mockMvc.perform(form(post(base + "/edit"), asClient()).param("rows[0].number", "0"))
                .andExpect(status().isOk()).andExpect(view().name("order/form"));
        mockMvc.perform(form(post("/orders/new"), asClient()).param("rows[0].applianceId", "999999").param("rows[0].number", "1"))
                .andExpect(status().isOk()).andExpect(view().name("order/form"));
        mockMvc.perform(form(post(base + "/approve"), asEmployee())).andExpect(status().is3xxRedirection());
        mockMvc.perform(form(post(base + "/delete"), asAdmin())).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/orders/deleted").with(user(asAdmin()))).andExpect(status().isOk());
        mockMvc.perform(form(post(base + "/restore"), asAdmin())).andExpect(status().is3xxRedirection());
    }

    @Test
    void clientManagesOwnAccount() throws Exception {
        mockMvc.perform(get("/account").with(user(asClient()))).andExpect(status().isOk());
        mockMvc.perform(get("/account").with(user(asEmployee()))).andExpect(status().isOk());
        mockMvc.perform(form(post("/account"), asClient()).param("name", "Magic 2").param("email", "magic@flow.test").param("card", "1234 5678"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(form(post("/account"), asClient()).param("name", "").param("email", "bad"))
                .andExpect(status().isOk());
        mockMvc.perform(form(post("/account"), asEmployee()).param("name", "x")).andExpect(status().isForbidden());
        mockMvc.perform(form(post("/account/deactivate"), asClient())).andExpect(redirectedUrl("/login?deleted"));
    }

    @Test
    void clientCanEraseOwnData() throws Exception {
        mockMvc.perform(form(post("/account/erase"), asClient())).andExpect(redirectedUrl("/login?erased"));
    }

    @Test
    void passwordChangeWorksAndRejectsWrongCurrentPassword() throws Exception {
        mockMvc.perform(get("/password/change").with(user(asClient()))).andExpect(status().isOk());
        mockMvc.perform(form(post("/password/change"), asClient())
                        .param("currentPassword", "wrong").param("newPassword", "N3w!Passw0rd#1").param("confirmPassword", "N3w!Passw0rd#1"))
                .andExpect(status().isOk());
        mockMvc.perform(form(post("/password/change"), asClient()).param("currentPassword", "").param("newPassword", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(form(post("/password/change"), asClient())
                        .param("currentPassword", PASSWORD).param("newPassword", "N3w!Passw0rd#1").param("confirmPassword", "N3w!Passw0rd#1"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(form(post("/password/change"), asEmployee())
                        .param("currentPassword", PASSWORD).param("newPassword", "N3w!Passw0rd#2").param("confirmPassword", "N3w!Passw0rd#2"))
                .andExpect(redirectedUrl("/"));
    }
}
