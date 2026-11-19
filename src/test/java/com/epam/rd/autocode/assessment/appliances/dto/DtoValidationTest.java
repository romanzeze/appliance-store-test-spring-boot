package com.epam.rd.autocode.assessment.appliances.dto;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static final String STRONG = "Strong-Pass-1!";

    private ClientRepository clientRepository;
    private EmployeeRepository employeeRepository;
    private AnnotationConfigApplicationContext context;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        clientRepository = Mockito.mock(ClientRepository.class);
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(ClientRepository.class, () -> clientRepository);
        context.registerBean(EmployeeRepository.class, () -> employeeRepository);
        context.refresh();
        validator = new LocalValidatorFactoryBean();
        validator.setApplicationContext(context);
        validator.afterPropertiesSet();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        validator.close();
        context.close();
        LocaleContextHolder.resetLocaleContext();
    }

    private ApplianceDto validAppliance() {
        return ApplianceDto.builder().name("Hair dryer").category(Category.SMALL).model("HP8230")
                .manufacturerId(4L).powerType(PowerType.AC220).power(2200).price(new BigDecimal("39.90")).build();
    }

    private RegistrationDto registration(String password, String confirmation) {
        return RegistrationDto.builder().name("Luka Doncic").email("luka@example.com")
                .password(password).confirmPassword(confirmation).card("5326-7705").build();
    }

    private Set<String> fieldsWithErrors(Object dto) {
        return validator.validate(dto).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private String messageFor(Object dto, String field) {
        return validator.validate(dto).stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage).findFirst().orElseThrow();
    }

    @Test
    void validApplianceHasNoViolations() {
        assertTrue(validator.validate(validAppliance()).isEmpty());
    }

    @Test
    void applianceRequiresFieldsAndPositiveNumbers() {
        ApplianceDto dto = validAppliance();
        dto.setName(" ");
        dto.setPower(0);
        dto.setPrice(new BigDecimal("-1"));
        dto.setManufacturerId(null);

        assertEquals(Set.of("name", "power", "price", "manufacturerId"), fieldsWithErrors(dto));
    }

    @Test
    void priceAllowsOnlyTwoDecimals() {
        ApplianceDto dto = validAppliance();
        dto.setPrice(new BigDecimal("10.999"));

        assertEquals(Set.of("price"), fieldsWithErrors(dto));
    }

    @Test
    void clientRequiresValidEmailAndCard() {
        ClientDto dto = ClientDto.builder().name("Luka Doncic").email("not-an-email").card("abc").build();

        assertEquals(Set.of("email", "card"), fieldsWithErrors(dto));
    }

    @Test
    void orderNeedsRowsWithAtLeastOneItem() {
        assertEquals(Set.of("rows"), fieldsWithErrors(new OrderDto()));

        OrderDto badRow = new OrderDto();
        badRow.setRows(List.of(OrderRowDto.builder().applianceId(1L).number(0L).build()));
        assertEquals(Set.of("rows[0].number"), fieldsWithErrors(badRow));
    }

    @Test
    void messagesFollowTheCurrentLocale() {
        ApplianceDto dto = new ApplianceDto();

        assertEquals("This field is required", messageFor(dto, "name"));
        LocaleContextHolder.setLocale(Locale.forLanguageTag("uk"));
        assertEquals("Це поле обов'язкове", messageFor(dto, "name"));
    }

    @Test
    void registrationWithStrongMatchingPasswordIsValid() {
        assertTrue(validator.validate(registration(STRONG, STRONG)).isEmpty());
    }

    @Test
    void registrationRejectsWeakPassword() {
        assertEquals(Set.of("password"), fieldsWithErrors(registration("password", "password")));
    }

    @Test
    void registrationRejectsMismatchedConfirmation() {
        assertEquals(Set.of("confirmPassword"), fieldsWithErrors(registration(STRONG, "Other-Pass-1!")));
    }

    @Test
    void registrationRejectsEmailUsedByActiveClient() {
        Mockito.when(clientRepository.findByEmailIgnoreCaseAndDeletedFalse("luka@example.com"))
                .thenReturn(Optional.of(new Client(7L, "Someone", "luka@example.com", "hash", "5326-0000")));

        assertEquals(Set.of("email"), fieldsWithErrors(registration(STRONG, STRONG)));
    }

    @Test
    void registrationRejectsEmailUsedByEmployee() {
        Mockito.when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("luka@example.com"))
                .thenReturn(Optional.of(new Employee(3L, "Staff", "luka@example.com", "hash", "sales")));

        assertEquals(Set.of("email"), fieldsWithErrors(registration(STRONG, STRONG)));
    }

    @Test
    void clientMayKeepOwnEmailButNotTakeAnotherOne() {
        Mockito.when(clientRepository.findByEmailIgnoreCaseAndDeletedFalse("luka@example.com"))
                .thenReturn(Optional.of(new Client(7L, "Luka", "luka@example.com", "hash", "5326-7705")));

        ClientDto own = ClientDto.builder().id(7L).name("Luka").email("luka@example.com").card("5326-7705").build();
        ClientDto other = ClientDto.builder().id(8L).name("Other").email("luka@example.com").card("5326-7705").build();

        assertTrue(validator.validate(own).isEmpty());
        assertEquals(Set.of("email"), fieldsWithErrors(other));
    }

    @Test
    void changePasswordChecksStrengthAndConfirmation() {
        ChangePasswordDto weak = new ChangePasswordDto("Old-Pass-1!", "short", "short");
        ChangePasswordDto mismatch = new ChangePasswordDto("Old-Pass-1!", STRONG, "Different-1!");
        ChangePasswordDto valid = new ChangePasswordDto("Old-Pass-1!", STRONG, STRONG);

        assertEquals(Set.of("newPassword"), fieldsWithErrors(weak));
        assertEquals(Set.of("confirmPassword"), fieldsWithErrors(mismatch));
        assertTrue(validator.validate(valid).isEmpty());
    }
}
