package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.ListModel;
import com.epam.rd.autocode.assessment.appliances.controller.support.PageParams;
import com.epam.rd.autocode.assessment.appliances.dto.audit.AuditLogDto;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private static final String PATH = "/audit";
    private static final Set<String> SORTABLE = Set.of("id", "createdAt", "action", "entityType");
    private static final List<String> ENTITY_TYPES = List.of("Appliance", "Client", "Employee", "Manufacturer", "Order", "ACCOUNT", "REQUEST");

    private final AuditService auditService;

    @GetMapping
    public String list(PageParams params,
                       @RequestParam(required = false, defaultValue = "") String action,
                       @RequestParam(required = false, defaultValue = "") String entityType,
                       Model model) {
        if (params.getDir() == null) {
            params.setDir("desc");
        }
        String safeAction = Arrays.stream(AuditAction.values()).map(Enum::name).anyMatch(action::equals) ? action : "";
        String safeEntity = ENTITY_TYPES.contains(entityType) ? entityType : "";
        Page<AuditLogDto> page = auditService.findAll(safeAction, safeEntity, params.toPageable(SORTABLE, "createdAt"));
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("action", safeAction);
        filters.put("entityType", safeEntity);
        ListModel.fill(model, PATH, params, SORTABLE, "createdAt", page, filters);
        model.addAttribute("items", page.getContent());
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("entityTypes", ENTITY_TYPES);
        model.addAttribute("action", safeAction);
        model.addAttribute("entityType", safeEntity);
        return "audit/list";
    }
}
