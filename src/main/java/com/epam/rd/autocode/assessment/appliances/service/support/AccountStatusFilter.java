package com.epam.rd.autocode.assessment.appliances.service.support;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.List;

@UtilityClass
public class AccountStatusFilter {

    public static final String ACTIVE = "ACTIVE";
    public static final String BLOCKED = "BLOCKED";
    public static final String MUST_CHANGE = "MUST_CHANGE";
    public static final List<String> VALUES = List.of(ACTIVE, BLOCKED, MUST_CHANGE);

    public String normalize(String status) {
        if (status == null) {
            return "";
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        return VALUES.contains(value) ? value : "";
    }
}
