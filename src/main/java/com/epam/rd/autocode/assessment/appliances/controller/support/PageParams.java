package com.epam.rd.autocode.assessment.appliances.controller.support;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

@Data
public class PageParams {

    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 4;

    private int page;
    private int size = DEFAULT_SIZE;
    private String sort;
    private String dir;
    private String query;
    private String status;

    public String sortProperty(Set<String> allowed, String defaultSort) {
        return sort != null && allowed.contains(sort) ? sort : defaultSort;
    }

    public Sort.Direction direction() {
        return "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    public int effectiveSize() {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public Pageable toPageable(Set<String> allowed, String defaultSort) {
        String property = sortProperty(allowed, defaultSort);
        Sort order = "status".equals(property)
                ? Sort.by(direction(), "blocked", "mustChangePassword")
                : Sort.by(direction(), property);
        if (!"id".equals(property)) {
            order = order.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(Math.max(page, 0), effectiveSize(), order);
    }
}
