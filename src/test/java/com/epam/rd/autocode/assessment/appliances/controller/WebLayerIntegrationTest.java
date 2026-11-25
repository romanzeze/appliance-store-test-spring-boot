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
import com.epam.rd.autocode.assessment.appliances.repository.AuditLogRepository;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebLayerIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ManufacturerService manufacturerService;
    @Autowired
    private ApplianceService applianceService;
    @Autowired
    private OrderService orderService;

    private Client client;
    private Client otherClient;
    private Employee employee;
    private Employee admin;
    private ApplianceDto appliance;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(new Client(null, "Magic", "magic@web.test", passwordEncoder.encode(PASSWORD), "1234 5678"));
        otherClient = clientRepository.save(new Client(null, "Larry", "larry@web.test", passwordEncoder.encode(PASSWORD), "8765 4321"));
        employee = employeeRepository.save(staff("Pippen", "pippen@web.test", Role.EMPLOYEE));
        admin = employeeRepository.save(staff("Kobe", "kobe@web.test", Role.ADMIN));
        Actor actor = new Actor(admin.getId(), Role.ADMIN);
        ManufacturerDto manufacturer = manufacturerService.create(ManufacturerDto.builder().name("Zeta Corp").build(), actor);
        appliance = applianceService.create(ApplianceDto.builder()
                .name("Zeta Fridge").category(Category.BIG).model("ZF-1").manufacturerId(manufacturer.getId())
                .powerType(PowerType.AC220).power(150).price(new BigDecimal("499.90")).build(), actor);
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
    void appliancesListShowsDataForClientWithOrderButton() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asClient())).param("query", "Zeta"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/list"))
                .andExpect(content().string(containsString("Zeta Fridge")))
                .andExpect(content().string(containsString("/cart/add")))
                .andExpect(content().string(not(containsString("/appliances/add"))));
    }

    @Test
    void anonymousVisitorSeesCatalogWithoutManagementButtons() throws Exception {
        mockMvc.perform(get("/appliances").param("query", "Zeta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zeta Fridge")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(not(containsString("/appliances/add"))))
                .andExpect(content().string(not(containsString("/delete"))))
                .andExpect(content().string(not(containsString("href=\"/orders\""))));
    }

    @Test
    void appliancesListShowsManagementButtonsForEmployee() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asEmployee())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/appliances/add")))
                .andExpect(content().string(containsString("/delete")));
    }

    @Test
    void unknownSortPropertyFallsBackToDefault() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asClient())).param("sort", "password").param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "name"))
                .andExpect(model().attribute("size", 100));
    }

    @Test
    void paginationAndSortingAreRendered() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asClient())).param("size", "1").param("sort", "price").param("dir", "desc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("page-link")))
                .andExpect(content().string(containsString("sort=price&amp;dir=asc")));
    }

    @Test
    void interfaceIsTranslatedWhenLanguageIsSwitched() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asClient())).param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Техніка")))
                .andExpect(content().string(containsString("href=\"/appliances?lang=en\"")));
        mockMvc.perform(get("/appliances").with(user(asClient())).param("lang", "en"))
                .andExpect(content().string(containsString("Appliances")));
    }

    @Test
    void menuFollowsConfiguredRoles() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asClient())))
                .andExpect(content().string(not(containsString("href=\"/manufacturers\""))))
                .andExpect(content().string(not(containsString("href=\"/employees\""))));
        mockMvc.perform(get("/appliances").with(user(asEmployee())))
                .andExpect(content().string(containsString("href=\"/manufacturers\"")))
                .andExpect(content().string(not(containsString("href=\"/employees\""))));
        mockMvc.perform(get("/appliances").with(user(asAdmin())))
                .andExpect(content().string(containsString("href=\"/manufacturers\"")))
                .andExpect(content().string(containsString("href=\"/employees\"")));
    }

    @Test
    void clientCannotOpenManufacturers() throws Exception {
        mockMvc.perform(get("/manufacturers").with(user(asClient()))).andExpect(status().isForbidden());
        mockMvc.perform(form(post("/appliances/add"), asClient())).andExpect(status().isForbidden());
    }

    @Test
    void employeeCreatesManufacturer() throws Exception {
        mockMvc.perform(form(post("/manufacturers/add"), asEmployee()).param("name", "Brand New"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manufacturers"));
        mockMvc.perform(get("/manufacturers").with(user(asEmployee())).param("query", "Brand"))
                .andExpect(content().string(containsString("Brand New")));
    }

    @Test
    void manufacturerFormShowsFieldError() throws Exception {
        mockMvc.perform(form(post("/manufacturers/add"), asEmployee()).param("name", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacturer/form"))
                .andExpect(content().string(containsString("is-invalid")))
                .andExpect(content().string(containsString("This field is required")));
    }

    @Test
    void duplicateManufacturerNameIsReportedOnTheField() throws Exception {
        mockMvc.perform(form(post("/manufacturers/add"), asEmployee()).param("name", "Zeta Corp"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    void validationMessagesFollowTheInterfaceLanguage() throws Exception {
        mockMvc.perform(form(post("/manufacturers/add"), asEmployee()).param("name", " ").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Це поле обов")));
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/manufacturers/add").with(user(asEmployee())).param("name", "X"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manufacturerInUseCannotBeDeletedAndErrorIsFlashed() throws Exception {
        Long id = manufacturerService.findAllActive().stream()
                .filter(item -> item.getName().equals("Zeta Corp")).findFirst().orElseThrow().getId();
        mockMvc.perform(form(post("/manufacturers/" + id + "/delete"), asEmployee()))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/4xx"))
                .andExpect(content().string(containsString("cannot be deleted")));
    }

    @Test
    void applianceFormValidatesFields() throws Exception {
        mockMvc.perform(form(post("/appliances/add"), asEmployee()).param("name", "").param("price", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/form"))
                .andExpect(content().string(containsString("is-invalid")));
    }

    @Test
    void employeeCreatesAppliance() throws Exception {
        Long manufacturerId = appliance.getManufacturerId();
        mockMvc.perform(form(post("/appliances/add"), asEmployee())
                        .param("name", "Zeta Oven").param("category", "BIG").param("model", "ZO-2")
                        .param("manufacturerId", manufacturerId.toString()).param("powerType", "AC220")
                        .param("power", "2000").param("price", "299.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));
    }

    @Test
    void formFieldsAreRenderedExactlyOnce() throws Exception {
        String registerPage = mockMvc.perform(get("/register")).andReturn().getResponse().getContentAsString();
        for (String field : new String[]{"name", "email", "card", "password", "confirmPassword"}) {
            org.assertj.core.api.Assertions.assertThat(registerPage.split("id=\"" + field + "\"", -1).length - 1)
                    .as("occurrences of field " + field).isEqualTo(1);
        }
        String applianceForm = mockMvc.perform(get("/appliances/add").with(user(asEmployee())))
                .andReturn().getResponse().getContentAsString();
        for (String field : new String[]{"name", "model", "characteristic", "description", "power", "price"}) {
            org.assertj.core.api.Assertions.assertThat(applianceForm.split("id=\"" + field + "\"", -1).length - 1)
                    .as("occurrences of field " + field).isEqualTo(1);
        }
    }

    @Test
    void registrationCreatesClientAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "New Client").param("email", "new@web.test").param("card", "1111 2222")
                        .param("password", PASSWORD).param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void registrationRejectsWeakPasswordMismatchAndTakenEmail() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "New").param("email", "magic@web.test").param("card", "1111 2222")
                        .param("password", "weak").param("confirmPassword", "other"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("already registered")))
                .andExpect(content().string(containsString("Password must be")))
                .andExpect(content().string(not(containsString("value=\"weak\""))));
    }

    @Test
    void loginPageShowsFailureMessages() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Invalid email or password")));
        mockMvc.perform(get("/login").param("blocked", ""))
                .andExpect(content().string(containsString("blocked")));
    }

    @Test
    void passwordChangeRejectsWrongCurrentPassword() throws Exception {
        mockMvc.perform(form(post("/password/change"), asClient())
                        .param("currentPassword", "Wrong!Pass123").param("newPassword", "N3w!Password99")
                        .param("confirmPassword", "N3w!Password99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("current password is incorrect")));
    }

    @Test
    void passwordChangeSucceedsAndRedirects() throws Exception {
        mockMvc.perform(form(post("/password/change"), asClient())
                        .param("currentPassword", PASSWORD).param("newPassword", "N3w!Password99")
                        .param("confirmPassword", "N3w!Password99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminCreatesEmployeeAndSeesTemporaryPasswordOnce() throws Exception {
        mockMvc.perform(form(post("/employees/add"), asAdmin())
                        .param("name", "Fresh").param("email", "fresh@web.test").param("department", "Support")
                        .param("role", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/credentials"))
                .andExpect(model().attributeExists("temporaryPassword"))
                .andExpect(content().string(containsString("shown only once")));
    }

    @Test
    void employeeCannotManageEmployees() throws Exception {
        mockMvc.perform(get("/employees").with(user(asEmployee()))).andExpect(status().isForbidden());
        mockMvc.perform(form(post("/employees/add"), asEmployee())).andExpect(status().isForbidden());
    }

    @Test
    void adminListsEmployees() throws Exception {
        mockMvc.perform(get("/employees").param("size", "50").with(user(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pippen@web.test")))
                .andExpect(content().string(containsString("reset-password")));
    }

    @Test
    void listsUseFourItemsPerPageByDefault() throws Exception {
        mockMvc.perform(get("/employees").with(user(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("size", 4))
                .andExpect(model().attribute("items", hasSize(lessThanOrEqualTo(4))));
    }

    @Test
    void employeesCanBeFilteredByStatus() throws Exception {
        mockMvc.perform(get("/employees").param("status", "BLOCKED").param("size", "50").with(user(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", "BLOCKED"))
                .andExpect(content().string(not(containsString("pippen@web.test"))));
        mockMvc.perform(get("/employees").param("status", "ACTIVE").param("size", "50").with(user(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pippen@web.test")));
    }

    @Test
    void unknownStatusFilterIsIgnored() throws Exception {
        mockMvc.perform(get("/clients").param("status", "bogus").with(user(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", ""));
    }

    @Test
    void adminCannotBlockOwnAccountAndGetsFlashedError() throws Exception {
        mockMvc.perform(form(post("/employees/" + admin.getId() + "/block"), asAdmin()))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/4xx"));
    }

    @Test
    void employeeBlocksAndUnblocksClient() throws Exception {
        mockMvc.perform(form(post("/clients/" + otherClient.getId() + "/block"), asEmployee()))
                .andExpect(redirectedUrl("/clients"));
        mockMvc.perform(get("/clients").with(user(asEmployee())))
                .andExpect(content().string(containsString("Blocked")));
        mockMvc.perform(form(post("/clients/" + otherClient.getId() + "/unblock"), asEmployee()))
                .andExpect(redirectedUrl("/clients"));
    }

    @Test
    void clientEditFormKeepsOwnershipOfEmailForUniquenessCheck() throws Exception {
        mockMvc.perform(form(post("/clients/" + client.getId() + "/edit"), asEmployee())
                        .param("name", "Magic Johnson").param("email", "magic@web.test").param("card", "1234 5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients"));
        mockMvc.perform(form(post("/clients/" + client.getId() + "/edit"), asEmployee())
                        .param("name", "Magic").param("email", "larry@web.test").param("card", "1234 5678"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already registered")));
    }

    @Test
    void erasingClientDataIsAdminOnly() throws Exception {
        mockMvc.perform(form(post("/clients/" + otherClient.getId() + "/erase"), asEmployee()))
                .andExpect(status().isForbidden());
        mockMvc.perform(form(post("/clients/" + otherClient.getId() + "/erase"), asAdmin()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void clientCreatesOrderAndSeesIt() throws Exception {
        mockMvc.perform(form(post("/orders/new"), asClient())
                        .param("rows[0].applianceId", appliance.getId().toString()).param("rows[0].number", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"));
        mockMvc.perform(get("/orders").with(user(asClient())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("999.80")));
    }

    @Test
    void orderWithoutRowsShowsValidationError() throws Exception {
        mockMvc.perform(form(post("/orders/new"), asClient()))
                .andExpect(status().isOk())
                .andExpect(view().name("order/form"))
                .andExpect(content().string(containsString("Add at least one item")));
    }

    @Test
    void orderFormListsAppliancesAndPreselectsOne() throws Exception {
        mockMvc.perform(get("/orders/new").with(user(asClient())).param("applianceId", appliance.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zeta Fridge")))
                .andExpect(content().string(containsString("order-row-template")));
    }

    @Test
    void clientCannotSeeAnotherClientsOrder() throws Exception {
        OrderDto order = orderService.create(OrderDto.builder().rows(List.of(
                OrderRowDto.builder().applianceId(appliance.getId()).number(1L).build())).build(),
                new Actor(otherClient.getId(), Role.USER));
        mockMvc.perform(get("/orders/" + order.getId()).with(user(asClient())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/orders/" + order.getId()).with(user(asEmployee())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zeta Fridge")));
    }

    @Test
    void onlyStaffApprovesOrders() throws Exception {
        OrderDto order = orderService.create(OrderDto.builder().rows(List.of(
                OrderRowDto.builder().applianceId(appliance.getId()).number(1L).build())).build(),
                new Actor(client.getId(), Role.USER));
        mockMvc.perform(form(post("/orders/" + order.getId() + "/approve"), asClient()))
                .andExpect(status().isForbidden());
        mockMvc.perform(form(post("/orders/" + order.getId() + "/approve"), asEmployee()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + order.getId()));
        mockMvc.perform(form(post("/orders/" + order.getId() + "/approve"), asEmployee()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("already approved")));
    }

    @Test
    void approvedOrderCannotBeEditedByClient() throws Exception {
        OrderDto order = orderService.create(OrderDto.builder().rows(List.of(
                OrderRowDto.builder().applianceId(appliance.getId()).number(1L).build())).build(),
                new Actor(client.getId(), Role.USER));
        orderService.approve(order.getId(), new Actor(employee.getId(), Role.EMPLOYEE));
        mockMvc.perform(form(post("/orders/" + order.getId() + "/edit"), asClient())
                        .param("rows[0].applianceId", appliance.getId().toString()).param("rows[0].number", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/form"))
                .andExpect(content().string(containsString("Approved orders cannot be changed")));
    }

    @Test
    void deletedViewsAreVisibleOnlyToAdmin() throws Exception {
        for (String path : new String[]{"/orders/deleted", "/appliances/deleted", "/clients/deleted", "/manufacturers/deleted"}) {
            mockMvc.perform(get(path).with(user(asClient()))).andExpect(status().isForbidden());
            mockMvc.perform(get(path).with(user(asEmployee()))).andExpect(status().isForbidden());
            mockMvc.perform(get(path).with(user(asAdmin()))).andExpect(status().isOk());
        }
    }

    @Test
    void showDeletedButtonIsHiddenFromEmployee() throws Exception {
        for (String path : new String[]{"/orders", "/appliances", "/clients", "/manufacturers"}) {
            mockMvc.perform(get(path).with(user(asEmployee())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(path + "/deleted"))));
            mockMvc.perform(get(path).with(user(asAdmin())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(path + "/deleted")));
        }
    }

    @Test
    void profilePageShowsClientFormAndEmployeeDetails() throws Exception {
        mockMvc.perform(get("/account").with(user(asClient())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("magic@web.test")))
                .andExpect(content().string(containsString("/account/erase")));
        mockMvc.perform(get("/account").with(user(asEmployee())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pippen@web.test")))
                .andExpect(content().string(not(containsString("/account/erase"))));
    }

    @Test
    void clientUpdatesOwnProfile() throws Exception {
        mockMvc.perform(form(post("/account"), asClient())
                        .param("name", "Magic J").param("email", "magic@web.test").param("card", "1234 5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
    }

    @Test
    void clientDeactivatesOwnAccount() throws Exception {
        mockMvc.perform(form(post("/account/deactivate"), asClient()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?deleted"));
        clientRepository.flush();
        org.assertj.core.api.Assertions.assertThat(
                clientRepository.findByIdAndDeletedFalse(client.getId())).isEmpty();
    }

    @Test
    void employeeCannotUseClientSelfService() throws Exception {
        mockMvc.perform(form(post("/account/deactivate"), asEmployee())).andExpect(status().isForbidden());
    }

    @Test
    void auditLogIsVisibleOnlyToAdmin() throws Exception {
        mockMvc.perform(get("/audit")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/audit").with(user(asClient()))).andExpect(status().isForbidden());
        mockMvc.perform(get("/audit").with(user(asEmployee()))).andExpect(status().isForbidden());
        mockMvc.perform(get("/audit").with(user(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(view().name("audit/list"));
    }

    @Test
    void serviceActionsArePersistedWithIdsOnly() {
        var rows = auditLogRepository.findAll();
        org.junit.jupiter.api.Assertions.assertTrue(rows.stream().anyMatch(a ->
                "CREATED".equals(a.getAction()) && "Manufacturer".equals(a.getEntityType())
                        && admin.getId().equals(a.getActorId()) && a.getActorRole() == Role.ADMIN && a.getCreatedAt() != null));
        org.junit.jupiter.api.Assertions.assertTrue(rows.stream().anyMatch(a ->
                "CREATED".equals(a.getAction()) && "Appliance".equals(a.getEntityType())));
    }

    @Test
    void auditLogCanBeFilteredByActionAndEntity() throws Exception {
        mockMvc.perform(get("/audit").with(user(asAdmin())).param("action", "CREATED").param("entityType", "Manufacturer").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("action", "CREATED"))
                .andExpect(model().attribute("entityType", "Manufacturer"))
                .andExpect(model().attribute("items", hasSize(1)));
        mockMvc.perform(get("/audit").with(user(asAdmin())).param("action", "DELETED").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(0)));
    }

    @Test
    void unknownAuditFiltersAreIgnoredAndListIsNewestFirstPaged() throws Exception {
        mockMvc.perform(get("/audit").with(user(asAdmin())).param("action", "x' or '1'='1").param("entityType", "zzz"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("action", ""))
                .andExpect(model().attribute("entityType", ""))
                .andExpect(model().attribute("dir", "desc"))
                .andExpect(model().attribute("items", hasSize(lessThanOrEqualTo(4))));
    }

    @Test
    void auditLinkIsShownOnlyToAdmin() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asEmployee())))
                .andExpect(content().string(not(containsString("href=\"/audit\""))));
        mockMvc.perform(get("/appliances").with(user(asAdmin())))
                .andExpect(content().string(containsString("href=\"/audit\"")));
    }

    @Test
    void cartAccumulatesItemsAcrossAdditionsAndFeedsOrderForm() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ApplianceDto second = applianceService.create(ApplianceDto.builder()
                .name("Zeta Kettle").category(Category.SMALL).model("ZK-1").manufacturerId(appliance.getManufacturerId())
                .powerType(PowerType.AC220).power(10).price(new BigDecimal("9.90")).build(), new Actor(admin.getId(), Role.ADMIN));
        mockMvc.perform(form(post("/cart/add"), asClient()).session(session).param("applianceId", appliance.getId().toString()).param("next", "/appliances?page=1"))
                .andExpect(redirectedUrl("/appliances?page=1"));
        mockMvc.perform(form(post("/cart/add"), asClient()).session(session).param("applianceId", appliance.getId().toString()))
                .andExpect(redirectedUrl("/appliances"));
        mockMvc.perform(form(post("/cart/add"), asClient()).session(session).param("applianceId", second.getId().toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/orders/new").with(user(asClient())).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("rows[0].applianceId")))
                .andExpect(content().string(containsString("rows[1].applianceId")))
                .andExpect(content().string(containsString("value=\"2\"")));
        mockMvc.perform(get("/appliances").with(user(asClient())).session(session))
                .andExpect(content().string(containsString("badge text-bg-success\">3<")));
        mockMvc.perform(form(post("/cart/clear"), asClient()).session(session)).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/appliances").with(user(asClient())).session(session))
                .andExpect(content().string(containsString("badge text-bg-success\">0<")));
    }

    @Test
    void cartRejectsExternalRedirectTargetsAndUnknownAppliances() throws Exception {
        mockMvc.perform(form(post("/cart/add"), asClient()).param("applianceId", appliance.getId().toString()).param("next", "https://evil.test"))
                .andExpect(redirectedUrl("/appliances"));
        mockMvc.perform(form(post("/cart/add"), asClient()).param("applianceId", appliance.getId().toString()).param("next", "//evil.test"))
                .andExpect(redirectedUrl("/appliances"));
        mockMvc.perform(form(post("/cart/add"), asClient()).param("applianceId", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulOrderEmptiesCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(form(post("/cart/add"), asClient()).session(session).param("applianceId", appliance.getId().toString()));
        mockMvc.perform(form(post("/orders/new"), asClient()).session(session)
                        .param("rows[0].applianceId", appliance.getId().toString()).param("rows[0].number", "1"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/appliances").with(user(asClient())).session(session))
                .andExpect(content().string(containsString("badge text-bg-success\">0<")));
    }

    @Test
    void guestsAreSentToLoginInsteadOfCart() throws Exception {
        mockMvc.perform(form(post("/cart/add"), asClient()).param("applianceId", "1")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/cart/add").with(csrf()).param("applianceId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void productPageIsPublicAndLocalized() throws Exception {
        applianceService.update(appliance.getId(), ApplianceDto.builder()
                .name("Zeta Fridge").category(Category.BIG).model("ZF-1").manufacturerId(appliance.getManufacturerId())
                .powerType(PowerType.AC220).power(150).price(new BigDecimal("499.90"))
                .nameUk("Холодильник Зета").descriptionUk("Опис українською").build(), new Actor(admin.getId(), Role.ADMIN));
        String path = "/appliances/" + appliance.getId();
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/details"))
                .andExpect(content().string(containsString("Zeta Fridge")))
                .andExpect(content().string(not(containsString("Холодильник Зета"))));
        mockMvc.perform(get(path).param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Холодильник Зета")))
                .andExpect(content().string(containsString("Опис українською")));
        mockMvc.perform(get("/appliances").param("lang", "uk").param("query", "Холодильник"))
                .andExpect(content().string(containsString("Холодильник Зета")));
        mockMvc.perform(get("/appliances/999999")).andExpect(status().isNotFound());
    }

    @Test
    void catalogSearchMatchesManufacturerName() throws Exception {
        mockMvc.perform(get("/appliances").param("query", "zeta corp"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zeta Fridge")));
        mockMvc.perform(get("/appliances").param("query", "nobody-makes-this"))
                .andExpect(content().string(not(containsString("Zeta Fridge"))));
    }

    @Test
    void manufacturerFilterNarrowsCatalog() throws Exception {
        mockMvc.perform(get("/appliances").param("manufacturerId", appliance.getManufacturerId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zeta Fridge")));
        mockMvc.perform(get("/appliances").param("manufacturerId", "999999"))
                .andExpect(content().string(not(containsString("Zeta Fridge"))));
    }

    @Test
    void contactFormIsPublicAndValidated() throws Exception {
        mockMvc.perform(get("/contact")).andExpect(status().isOk()).andExpect(view().name("contact/form"));
        mockMvc.perform(post("/contact").with(csrf()).param("name", "").param("email", "bad").param("message", ""))
                .andExpect(status().isOk()).andExpect(view().name("contact/form"));
        mockMvc.perform(post("/contact").with(csrf()).param("name", "Guest").param("email", "guest@web.test").param("message", "Hello"))
                .andExpect(redirectedUrl("/contact"));
    }

    @Test
    void contactFormIsPrefilledForSignedInClient() throws Exception {
        mockMvc.perform(get("/contact").with(user(asClient())))
                .andExpect(content().string(containsString("magic@web.test")))
                .andExpect(content().string(containsString("Magic")));
        mockMvc.perform(form(post("/contact"), asClient()).param("name", "Magic").param("email", "magic@web.test").param("message", "Hi"))
                .andExpect(redirectedUrl("/contact"));
    }

    @Test
    void footerLinksToContactForm() throws Exception {
        mockMvc.perform(get("/")).andExpect(content().string(containsString("href=\"/contact\"")));
    }

    @Test
    void navbarShowsAccountHolderName() throws Exception {
        mockMvc.perform(get("/appliances").with(user(asClient())))
                .andExpect(content().string(containsString(">Magic</a>")));
    }

    @Test
    void requiredFieldsAreMarkedAndCardInputIsMasked() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(content().string(containsString("required-mark")))
                .andExpect(content().string(containsString("data-card-mask")))
                .andExpect(content().string(containsString("inputmode=\"numeric\"")));
    }

    @Test
    void auditLogDoesNotContainClientActions() throws Exception {
        orderService.create(OrderDto.builder()
                .rows(new java.util.ArrayList<>(List.of(OrderRowDto.builder().applianceId(appliance.getId()).number(1L).build())))
                .build(), new Actor(client.getId(), Role.USER));
        org.junit.jupiter.api.Assertions.assertTrue(auditLogRepository.findAll().stream()
                .noneMatch(a -> a.getActorRole() == Role.USER));
        mockMvc.perform(get("/audit").with(user(asAdmin())).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    List<?> items = (List<?>) result.getModelAndView().getModel().get("items");
                    org.junit.jupiter.api.Assertions.assertFalse(items.isEmpty());
                    items.forEach(item -> org.junit.jupiter.api.Assertions.assertNotEquals(Role.USER,
                            ((com.epam.rd.autocode.assessment.appliances.dto.audit.AuditLogDto) item).actorRole()));
                });
    }

    @Test
    void staffCanSearchClientsPagedForOrderForm() throws Exception {
        mockMvc.perform(get("/clients/search").with(user(asEmployee())).param("query", client.getName().substring(0, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(client.getId()))
                .andExpect(jsonPath("$.page").value(0));
        mockMvc.perform(get("/clients/search").with(user(asEmployee())).param("query", "zzz-no-such-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mockMvc.perform(get("/clients/search").with(user(asClient())))
                .andExpect(status().isForbidden());
    }

    @Test
    void orderFormShowsClientPickerForStaff() throws Exception {
        mockMvc.perform(get("/orders/new").with(user(asEmployee())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client-picker")));
    }

    @Test
    void clientsCanBeSortedByStatusActiveFirstThenBlocked() throws Exception {
        mockMvc.perform(form(post("/clients/" + client.getId() + "/block"), asEmployee()));
        java.util.function.Function<String, List<Boolean>> blockedOrder = dir -> {
            try {
                var result = mockMvc.perform(get("/clients").with(user(asEmployee())).param("sort", "status").param("dir", dir).param("size", "50"))
                        .andExpect(status().isOk()).andReturn();
                List<?> items = (List<?>) result.getModelAndView().getModel().get("items");
                return items.stream().map(i -> ((com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto) i).isBlocked()).toList();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        List<Boolean> asc = blockedOrder.apply("asc");
        List<Boolean> desc = blockedOrder.apply("desc");
        org.junit.jupiter.api.Assertions.assertTrue(asc.contains(true));
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, asc.get(asc.size() - 1));
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, asc.get(0));
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, desc.get(0));
    }

    @Test
    void employeesCanBeSortedByStatus() throws Exception {
        mockMvc.perform(get("/employees").with(user(asAdmin())).param("sort", "status").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "status"))
                .andExpect(content().string(containsString("sort=status")));
    }
}
