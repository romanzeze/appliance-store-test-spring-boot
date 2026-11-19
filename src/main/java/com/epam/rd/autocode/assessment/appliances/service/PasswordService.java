package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.user.ChangePasswordDto;
import com.epam.rd.autocode.assessment.appliances.exception.OperationNotAllowedException;
import com.epam.rd.autocode.assessment.appliances.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*-_=+?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int TEMPORARY_LENGTH = 14;
    private static final Duration TEMPORARY_VALIDITY = Duration.ofHours(48);

    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public String generateTemporaryPassword() {
        List<Character> symbols = new ArrayList<>();
        symbols.add(pick(UPPER));
        symbols.add(pick(LOWER));
        symbols.add(pick(DIGITS));
        symbols.add(pick(SPECIAL));
        while (symbols.size() < TEMPORARY_LENGTH) {
            symbols.add(pick(ALL));
        }
        Collections.shuffle(symbols, random);
        StringBuilder password = new StringBuilder();
        symbols.forEach(password::append);
        return password.toString();
    }

    public void applyTemporaryPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setMustChangePassword(true);
        user.setPasswordExpiresAt(LocalDateTime.now(clock).plus(TEMPORARY_VALIDITY));
    }

    public void changePassword(User user, ChangePasswordDto dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new OperationNotAllowedException("error.password.currentWrong");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new OperationNotAllowedException("error.password.same");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setMustChangePassword(false);
        user.setPasswordExpiresAt(null);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
