package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceTranslationRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.impl.ApplianceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplianceServiceTest {

    private static final Actor STAFF = new Actor(1L, Role.EMPLOYEE);

    @Mock
    private ApplianceRepository applianceRepository;
    @Mock
    private ManufacturerRepository manufacturerRepository;
    @Mock
    private ApplianceTranslationRepository translationRepository;
    @Mock
    private AuditService auditService;

    private ApplianceServiceImpl service;
    private Manufacturer bosch;

    @BeforeEach
    void setUp() {
        service = new ApplianceServiceImpl(applianceRepository, manufacturerRepository, translationRepository,
                ServiceTestSupport.mapper(), auditService);
        bosch = new Manufacturer(3L, "Bosch");
    }

    private ApplianceDto dto(Long manufacturerId) {
        return ApplianceDto.builder().name("Washer").category(Category.BIG).model("S6").manufacturerId(manufacturerId)
                .powerType(PowerType.AC220).power(2300).price(new BigDecimal("649.00")).build();
    }

    private Appliance appliance() {
        return new Appliance(10L, "Washer", Category.BIG, "S6", bosch, PowerType.AC220, null, null, 2300,
                new BigDecimal("649.00"));
    }

    @Test
    void createLinksManufacturerAndAudits() {
        when(manufacturerRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(bosch));
        when(applianceRepository.save(any(Appliance.class))).thenAnswer(call -> {
            Appliance saved = call.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ApplianceDto result = service.create(dto(3L), STAFF);

        assertEquals(10L, result.getId());
        assertEquals("Bosch", result.getManufacturerName());
        verify(auditService).record(STAFF, AuditAction.CREATED, "Appliance", 10L);
    }

    @Test
    void createFailsForUnknownManufacturer() {
        when(manufacturerRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(dto(99L), STAFF));
        verify(applianceRepository, never()).save(any());
    }

    @Test
    void updateChangesFieldsAndManufacturer() {
        Manufacturer samsung = new Manufacturer(4L, "Samsung");
        Appliance existing = appliance();
        when(applianceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(existing));
        when(manufacturerRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(samsung));
        when(applianceRepository.save(existing)).thenReturn(existing);
        ApplianceDto changes = dto(4L);
        changes.setName("Washer Pro");
        changes.setPrice(new BigDecimal("799.00"));

        ApplianceDto result = service.update(10L, changes, STAFF);

        assertEquals("Washer Pro", result.getName());
        assertEquals(new BigDecimal("799.00"), result.getPrice());
        assertEquals("Samsung", result.getManufacturerName());
        assertEquals(10L, result.getId());
    }

    @Test
    void deleteIsSoftAndAudited() {
        Appliance existing = appliance();
        when(applianceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(existing));

        service.delete(10L, STAFF);

        verify(applianceRepository).delete(existing);
        verify(auditService).record(STAFF, AuditAction.DELETED, "Appliance", 10L);
    }

    @Test
    void restoreIsRefusedWhenManufacturerIsDeleted() {
        Appliance deleted = appliance();
        deleted.setDeleted(true);
        bosch.setDeleted(true);
        when(applianceRepository.findById(10L)).thenReturn(Optional.of(deleted));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.restore(10L, STAFF));

        assertEquals("error.appliance.manufacturerDeleted", exception.getMessageKey());
    }

    @Test
    void restoreBringsApplianceBack() {
        Appliance deleted = appliance();
        deleted.setDeleted(true);
        when(applianceRepository.findById(10L)).thenReturn(Optional.of(deleted));

        service.restore(10L, STAFF);

        verify(applianceRepository).restore(10L);
        verify(auditService).record(STAFF, AuditAction.RESTORED, "Appliance", 10L);
    }

    @Test
    void clientsCannotModifyAppliances() {
        Actor client = new Actor(3L, Role.USER);

        assertThrows(AccessDeniedException.class, () -> service.create(dto(3L), client));
        assertThrows(AccessDeniedException.class, () -> service.update(10L, dto(3L), client));
        assertThrows(AccessDeniedException.class, () -> service.delete(10L, client));
    }

    @Test
    void createStoresUkrainianTranslation() {
        when(manufacturerRepository.findByIdAndDeletedFalse(3L)).thenReturn(java.util.Optional.of(bosch));
        when(applianceRepository.save(any(Appliance.class))).thenAnswer(invocation -> {
            Appliance saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        ApplianceDto request = dto(3L);
        request.setNameUk("Пральна машина");
        ApplianceDto created = service.create(request, STAFF);
        org.mockito.ArgumentCaptor<com.epam.rd.autocode.assessment.appliances.model.ApplianceTranslation> captor =
                org.mockito.ArgumentCaptor.forClass(com.epam.rd.autocode.assessment.appliances.model.ApplianceTranslation.class);
        verify(translationRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Пральна машина", captor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals(11L, captor.getValue().getApplianceId());
        org.junit.jupiter.api.Assertions.assertEquals("Пральна машина", created.displayName("uk"));
        org.junit.jupiter.api.Assertions.assertEquals("Washer", created.displayName("en"));
    }

    @Test
    void createWithoutTranslationDoesNotStoreOne() {
        when(manufacturerRepository.findByIdAndDeletedFalse(3L)).thenReturn(java.util.Optional.of(bosch));
        when(applianceRepository.save(any(Appliance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(dto(3L), STAFF);
        verify(translationRepository, never()).save(any());
    }

    @Test
    void displayFallsBackToDefaultWhenTranslationIsMissing() {
        ApplianceDto dto = dto(3L);
        org.junit.jupiter.api.Assertions.assertEquals("Washer", dto.displayName("uk"));
        org.junit.jupiter.api.Assertions.assertNull(dto.displayDescription("uk"));
    }
}
