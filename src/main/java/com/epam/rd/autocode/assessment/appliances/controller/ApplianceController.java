package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.controller.support.ListModel;
import com.epam.rd.autocode.assessment.appliances.controller.support.PageParams;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceDto;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceFilterDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/appliances")
@RequiredArgsConstructor
public class ApplianceController {

    private static final String PATH = "/appliances";
    private static final String LIST_VIEW = "appliance/list";
    private static final String FORM_VIEW = "appliance/form";
    private static final String DETAILS_VIEW = "appliance/details";
    private static final Set<String> SORTABLE = Set.of("id", "name", "category", "model", "power", "price");

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;
    private final CurrentUserService currentUserService;

    @InitBinder("appliance")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "category", "model", "manufacturerId", "powerType",
                "characteristic", "description", "power", "price", "nameUk", "characteristicUk", "descriptionUk");
    }

    @GetMapping
    public String list(PageParams params, @ModelAttribute("filter") ApplianceFilterDto filter, Model model,
                       HttpServletRequest request) {
        Page<ApplianceDto> page = applianceService.search(filter, params.toPageable(SORTABLE, "name"));
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("query", filter.getQuery());
        filters.put("category", filter.getCategory());
        filters.put("manufacturerId", filter.getManufacturerId());
        filters.put("powerType", filter.getPowerType());
        ListModel.fill(model, PATH, params, SORTABLE, "name", page, filters);
        model.addAttribute("items", page.getContent());
        model.addAttribute("manufacturers", manufacturerService.findAllActive());
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("deletedView", false);
        String queryString = request.getQueryString();
        model.addAttribute("returnTo", queryString == null ? PATH : PATH + "?" + queryString);
        return LIST_VIEW;
    }
    @GetMapping("/{id:\\d+}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("item", applianceService.findById(id));
        return DETAILS_VIEW;
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleted(PageParams params, Model model) {
        Page<ApplianceDto> page = applianceService.findDeleted(params.toPageable(SORTABLE, "name"));
        ListModel.fill(model, PATH + "/deleted", params, SORTABLE, "name", page, Map.of());
        model.addAttribute("items", page.getContent());
        model.addAttribute("deletedView", true);
        return LIST_VIEW;
    }
    @GetMapping("/add")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String addForm(Model model) {
        model.addAttribute("appliance", new ApplianceDto());
        return form(model, PATH + "/add");
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String create(@Valid @ModelAttribute("appliance") ApplianceDto dto, BindingResult result,
                         Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return form(model, PATH + "/add");
        }
        try {
            applianceService.create(dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return form(model, PATH + "/add");
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH;
    }
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("appliance", applianceService.findById(id));
        return form(model, PATH + "/" + id + "/edit");
    }
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("appliance") ApplianceDto dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return form(model, PATH + "/" + id + "/edit");
        }
        try {
            applianceService.update(id, dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return form(model, PATH + "/" + id + "/edit");
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH;
    }
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applianceService.delete(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.deleted");
        return "redirect:" + PATH;
    }
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applianceService.restore(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.restored");
        return "redirect:" + PATH + "/deleted";
    }

    private String form(Model model, String action) {
        model.addAttribute("formAction", action);
        model.addAttribute("manufacturers", manufacturerService.findAllActive());
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        return FORM_VIEW;
    }
}
