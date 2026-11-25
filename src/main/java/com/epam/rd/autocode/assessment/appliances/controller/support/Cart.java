package com.epam.rd.autocode.assessment.appliances.controller.support;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("cart")
@SessionScope
public class Cart {

    private final Map<Long, Long> items = new LinkedHashMap<>();

    public synchronized void add(Long applianceId) {
        items.merge(applianceId, 1L, Long::sum);
    }

    public synchronized void clear() {
        items.clear();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized long count() {
        return items.values().stream().mapToLong(Long::longValue).sum();
    }

    public synchronized List<OrderRowDto> rows() {
        List<OrderRowDto> rows = new ArrayList<>();
        items.forEach((id, number) -> rows.add(OrderRowDto.builder().applianceId(id).number(number).build()));
        return rows;
    }
}
