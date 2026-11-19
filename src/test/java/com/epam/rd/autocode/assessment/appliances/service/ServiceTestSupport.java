package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.config.MainConfig;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class ServiceTestSupport {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);

    private ServiceTestSupport() {
    }

    public static ModelMapper mapper() {
        return new MainConfig().modelMapper();
    }

    public static PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(4);
    }

    public static PasswordService passwordService() {
        return new PasswordService(encoder(), CLOCK);
    }
}
