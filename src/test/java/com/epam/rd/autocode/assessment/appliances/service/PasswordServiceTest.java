package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.validation.StrongPasswordValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService service = ServiceTestSupport.passwordService();

    @Test
    void temporaryPasswordsAreStrongAndDifferent() {
        StrongPasswordValidator validator = new StrongPasswordValidator();
        String first = service.generateTemporaryPassword();
        String second = service.generateTemporaryPassword();

        assertEquals(14, first.length());
        assertTrue(validator.isValid(first, null));
        assertTrue(validator.isValid(second, null));
        assertNotEquals(first, second);
    }

    @Test
    void temporaryPasswordIsHashedAndExpiresInTwoDays() {
        Client client = new Client(1L, "Name", "a@b.com", "x", "5326-1111");

        service.applyTemporaryPassword(client, "Temp-Pass-1!");

        assertTrue(client.isMustChangePassword());
        assertNotEquals("Temp-Pass-1!", client.getPassword());
        assertTrue(ServiceTestSupport.encoder().matches("Temp-Pass-1!", client.getPassword()));
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 0), client.getPasswordExpiresAt());
    }

    @Test
    void changePasswordClearsTemporaryFlags() {
        Client client = new Client(1L, "Name", "a@b.com", service.encode("Old-Pass-1!"), "5326-1111");
        client.setMustChangePassword(true);
        client.setPasswordExpiresAt(LocalDateTime.now());

        service.changePassword(client, new ChangePasswordDto("Old-Pass-1!", "New-Pass-2@", "New-Pass-2@"));

        assertTrue(ServiceTestSupport.encoder().matches("New-Pass-2@", client.getPassword()));
        assertFalse(client.isMustChangePassword());
        assertNull(client.getPasswordExpiresAt());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        Client client = new Client(1L, "Name", "a@b.com", service.encode("Old-Pass-1!"), "5326-1111");

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.changePassword(client, new ChangePasswordDto("Wrong-1!", "New-Pass-2@", "New-Pass-2@")));

        assertEquals("error.password.currentWrong", exception.getMessageKey());
    }

    @Test
    void changePasswordRejectsReusingTheSamePassword() {
        Client client = new Client(1L, "Name", "a@b.com", service.encode("Old-Pass-1!"), "5326-1111");

        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                () -> service.changePassword(client, new ChangePasswordDto("Old-Pass-1!", "Old-Pass-1!", "Old-Pass-1!")));

        assertEquals("error.password.same", exception.getMessageKey());
    }
}
