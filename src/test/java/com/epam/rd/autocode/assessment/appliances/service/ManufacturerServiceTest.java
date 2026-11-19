package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.impl.ManufacturerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {

    private static final Actor STAFF = new Actor(1L, Role.EMPLOYEE);

    @Mock
    private ManufacturerRepository manufacturerRepository;
    @Mock
    private ApplianceRepository applianceRepository;
    @Mock
    private AuditService auditService;

    private ManufacturerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ManufacturerServiceImpl(manufacturerRepository, applianceRepository,
                ServiceTestSupport.mapper(), auditService);
    }

    @Test
    void createTrimsNameSavesAndAudits() {
        when(manufacturerRepository.findByNameIgnoreCaseAndDeletedFalse("Bosch")).thenReturn(Optional.empty());
        when(manufacturerRepository.save(any(Manufacturer.class))).thenAnswer(call -> {
            Manufacturer saved = call.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        ManufacturerDto result = service.create(new ManufacturerDto(null, "  Bosch "), STAFF);

        assertEquals(5L, result.getId());
        assertEquals("Bosch", result.getName());
        verify(auditService).record(STAFF, AuditAction.CREATED, "Manufacturer", 5L);
    }

    @Test
    void createRejectsDuplicateName() {
        when(manufacturerRepository.findByNameIgnoreCaseAndDeletedFalse("Bosch"))
                .thenReturn(Optional.of(new Manufacturer(2L, "Bosch")));

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.create(new ManufacturerDto(null, "Bosch"), STAFF));

        assertEquals("error.manufacturer.nameTaken", exception.getMessageKey());
        verify(manufacturerRepository, never()).save(any());
    }

    @Test
    void updateMayKeepItsOwnName() {
        Manufacturer existing = new Manufacturer(2L, "Bosch");
        when(manufacturerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(existing));
        when(manufacturerRepository.findByNameIgnoreCaseAndDeletedFalse("BOSCH")).thenReturn(Optional.of(existing));
        when(manufacturerRepository.save(existing)).thenReturn(existing);

        ManufacturerDto result = service.update(2L, new ManufacturerDto(null, "BOSCH"), STAFF);

        assertEquals("BOSCH", result.getName());
    }

    @Test
    void deleteIsRefusedWhileApplianceExists() {
        when(manufacturerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(new Manufacturer(2L, "Bosch")));
        when(applianceRepository.existsByManufacturerIdAndDeletedFalse(2L)).thenReturn(true);

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.delete(2L, STAFF));

        assertEquals("error.manufacturer.inUse", exception.getMessageKey());
        verify(manufacturerRepository, never()).delete(any());
    }

    @Test
    void deleteSoftDeletesAndAudits() {
        Manufacturer manufacturer = new Manufacturer(2L, "Bosch");
        when(manufacturerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(manufacturer));
        when(applianceRepository.existsByManufacturerIdAndDeletedFalse(2L)).thenReturn(false);

        service.delete(2L, STAFF);

        verify(manufacturerRepository).delete(manufacturer);
        verify(auditService).record(STAFF, AuditAction.DELETED, "Manufacturer", 2L);
    }

    @Test
    void restoreChecksNameIsStillFree() {
        Manufacturer deleted = new Manufacturer(2L, "Bosch");
        deleted.setDeleted(true);
        when(manufacturerRepository.findById(2L)).thenReturn(Optional.of(deleted));
        when(manufacturerRepository.findByNameIgnoreCaseAndDeletedFalse("Bosch"))
                .thenReturn(Optional.of(new Manufacturer(9L, "Bosch")));

        assertThrows(OperationNotAllowedException.class, () -> service.restore(2L, STAFF));
        verify(manufacturerRepository, never()).restore(any());
    }

    @Test
    void restoreBringsDeletedManufacturerBack() {
        Manufacturer deleted = new Manufacturer(2L, "Bosch");
        deleted.setDeleted(true);
        when(manufacturerRepository.findById(2L)).thenReturn(Optional.of(deleted));
        when(manufacturerRepository.findByNameIgnoreCaseAndDeletedFalse("Bosch")).thenReturn(Optional.empty());

        service.restore(2L, STAFF);

        verify(manufacturerRepository).restore(2L);
        verify(auditService).record(STAFF, AuditAction.RESTORED, "Manufacturer", 2L);
    }

    @Test
    void findByIdThrowsWhenMissingOrDeleted() {
        when(manufacturerRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(9L));
    }

    @Test
    void clientsCannotModifyManufacturers() {
        Actor client = new Actor(3L, Role.USER);

        assertThrows(AccessDeniedException.class, () -> service.create(new ManufacturerDto(null, "X"), client));
        assertThrows(AccessDeniedException.class, () -> service.delete(1L, client));
    }

    @Test
    void searchUsesQueryWhenGiven() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(manufacturerRepository.search("bos", pageable))
                .thenReturn(new PageImpl<>(List.of(new Manufacturer(2L, "Bosch"))));
        when(manufacturerRepository.findByDeletedFalse(pageable)).thenReturn(new PageImpl<>(List.of()));

        assertEquals(1, service.findAll(" bos ", pageable).getTotalElements());
        assertEquals(0, service.findAll("  ", pageable).getTotalElements());
    }
}
