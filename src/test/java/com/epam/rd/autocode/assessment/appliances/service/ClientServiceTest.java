package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.RegistrationDto;
import com.epam.rd.autocode.assessment.appliances.exception.DuplicateEmailException;
import com.epam.rd.autocode.assessment.appliances.exception.ResourceNotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.service.impl.ClientServiceImpl;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private static final Actor STAFF = new Actor(50L, Role.EMPLOYEE);
    private static final Actor OWNER = new Actor(7L, Role.USER);
    private static final Actor STRANGER = new Actor(8L, Role.USER);

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmailAvailabilityService emailAvailabilityService;
    @Mock
    private AuditService auditService;
    @Mock
    private UserSessionService userSessionService;

    private ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(clientRepository, emailAvailabilityService, ServiceTestSupport.passwordService(),
                ServiceTestSupport.mapper(), auditService, userSessionService);
    }

    private Client client() {
        return new Client(7L, "Luka Doncic", "luka@example.com", ServiceTestSupport.encoder().encode("Old-Pass-1!"),
                "5326-7705");
    }

    @Test
    void registerHashesPasswordNormalizesEmailAndAssignsUserRole() {
        when(emailAvailabilityService.isTaken("luka@example.com", AccountType.CLIENT, null)).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> {
            Client saved = call.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        RegistrationDto dto = new RegistrationDto("Luka Doncic", "  Luka@Example.COM ", "Plain-Pass-1!", "Plain-Pass-1!",
                "5326-7705");

        ClientDto result = service.register(dto);

        assertEquals(7L, result.getId());
        assertEquals("luka@example.com", result.getEmail());
        verify(clientRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getRole() == Role.USER
                        && !saved.getPassword().equals("Plain-Pass-1!")
                        && ServiceTestSupport.encoder().matches("Plain-Pass-1!", saved.getPassword())));
        verify(auditService).record(new Actor(7L, Role.USER), AuditAction.CREATED, "Client", 7L);
    }

    @Test
    void registerRejectsTakenEmail() {
        when(emailAvailabilityService.isTaken("luka@example.com", AccountType.CLIENT, null)).thenReturn(true);
        RegistrationDto dto = new RegistrationDto("Luka", "luka@example.com", "Plain-Pass-1!", "Plain-Pass-1!", "5326-7705");

        assertThrows(DuplicateEmailException.class, () -> service.register(dto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void ownerCanUpdateProfileButSecurityFieldsStayUntouched() {
        Client existing = client();
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));
        when(emailAvailabilityService.isTaken("new@example.com", AccountType.CLIENT, 7L)).thenReturn(false);
        when(clientRepository.save(existing)).thenReturn(existing);
        ClientDto dto = ClientDto.builder().name("Luka D.").email("New@Example.com").card("5326-0000").blocked(true).build();

        ClientDto result = service.update(7L, dto, OWNER);

        assertEquals("Luka D.", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertFalse(existing.isBlocked());
        assertEquals(Role.USER, existing.getRole());
    }

    @Test
    void strangerCannotUpdateAnotherClient() {
        assertThrows(AccessDeniedException.class,
                () -> service.update(7L, ClientDto.builder().email("a@b.com").build(), STRANGER));
    }

    @Test
    void updateRejectsEmailOfSomeoneElse() {
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(client()));
        when(emailAvailabilityService.isTaken("taken@example.com", AccountType.CLIENT, 7L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> service.update(7L, ClientDto.builder().name("x").email("taken@example.com").build(), STAFF));
    }

    @Test
    void ownerChangesPassword() {
        Client existing = client();
        existing.setMustChangePassword(true);
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        service.changePassword(7L, new ChangePasswordDto("Old-Pass-1!", "New-Pass-2@", "New-Pass-2@"), OWNER);

        assertTrue(ServiceTestSupport.encoder().matches("New-Pass-2@", existing.getPassword()));
        assertFalse(existing.isMustChangePassword());
        verify(auditService).record(OWNER, AuditAction.PASSWORD_CHANGED, "Client", 7L);
    }

    @Test
    void staffCannotChangeClientPassword() {
        assertThrows(AccessDeniedException.class,
                () -> service.changePassword(7L, new ChangePasswordDto("a", "b", "b"), STAFF));
    }

    @Test
    void blockEndsActiveSessions() {
        Client existing = client();
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        service.block(7L, STAFF);

        assertTrue(existing.isBlocked());
        verify(userSessionService).expireSessions(AccountType.CLIENT, 7L);
        verify(auditService).record(STAFF, AuditAction.BLOCKED, "Client", 7L);
    }

    @Test
    void clientsCannotBlockAnyone() {
        assertThrows(AccessDeniedException.class, () -> service.block(8L, OWNER));
    }

    @Test
    void unblockClearsFlag() {
        Client existing = client();
        existing.setBlocked(true);
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        service.unblock(7L, STAFF);

        assertFalse(existing.isBlocked());
        verify(auditService).record(STAFF, AuditAction.UNBLOCKED, "Client", 7L);
    }

    @Test
    void deactivateIsSoftAndEndsSessions() {
        Client existing = client();
        when(clientRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        service.deactivate(7L, OWNER);

        verify(clientRepository).delete(existing);
        verify(clientRepository, never()).hardDeleteById(any());
        verify(userSessionService).expireSessions(AccountType.CLIENT, 7L);
        verify(auditService).record(OWNER, AuditAction.ACCOUNT_DEACTIVATED, "Client", 7L);
    }

    @Test
    void strangerCannotDeactivate() {
        assertThrows(AccessDeniedException.class, () -> service.deactivate(7L, STRANGER));
    }

    @Test
    void restoreFailsWhenEmailWasTakenInTheMeantime() {
        Client deleted = client();
        deleted.setDeleted(true);
        when(clientRepository.findById(7L)).thenReturn(Optional.of(deleted));
        when(emailAvailabilityService.isTaken("luka@example.com", AccountType.CLIENT, 7L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.restore(7L, STAFF));
        verify(clientRepository, never()).restore(any());
    }

    @Test
    void restoreBringsClientBack() {
        Client deleted = client();
        deleted.setDeleted(true);
        when(clientRepository.findById(7L)).thenReturn(Optional.of(deleted));
        when(emailAvailabilityService.isTaken("luka@example.com", AccountType.CLIENT, 7L)).thenReturn(false);

        service.restore(7L, STAFF);

        verify(clientRepository).restore(7L);
        verify(auditService).record(STAFF, AuditAction.RESTORED, "Client", 7L);
    }

    @Test
    void eraseRecordsEventBeforeDeletingAndEndsSessions() {
        when(clientRepository.existsById(7L)).thenReturn(true);

        service.eraseAllData(7L, OWNER);

        InOrder order = inOrder(auditService, clientRepository, userSessionService);
        order.verify(auditService).record(OWNER, AuditAction.ACCOUNT_ERASED, "Client", 7L);
        order.verify(clientRepository).hardDeleteById(7L);
        order.verify(userSessionService).expireSessions(AccountType.CLIENT, 7L);
    }

    @Test
    void eraseUnknownClientFails() {
        when(clientRepository.existsById(7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eraseAllData(7L, STAFF));
        verify(clientRepository, never()).hardDeleteById(any());
    }

    @Test
    void strangerCannotEraseAnotherClient() {
        assertThrows(AccessDeniedException.class, () -> service.eraseAllData(7L, STRANGER));
    }
}
