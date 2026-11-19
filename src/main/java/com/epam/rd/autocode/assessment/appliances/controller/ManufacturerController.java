package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.controller.support.ListModel;
import com.epam.rd.autocode.assessment.appliances.controller.support.PageParams;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private static final String PATH = "/manufacturers";
    private static final String LIST_VIEW = "manufacturer/list";
    private static final String FORM_VIEW = "manufacturer/form";
    private static final Set<String> SORTABLE = Set.of("id", "name");

    private final ManufacturerService manufacturerService;
    private final CurrentUserService currentUserService;

    @InitBinder("manufacturer")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("name");
    }

    @GetMapping
    public String list(PageParams params, Model model) {
        Page<ManufacturerDto> page = manufacturerService.findAll(params.getQuery(),
                params.toPageable(SORTABLE, "name"));
        ListModel.fill(model, PATH, params, SORTABLE, "name", page, Map.of("query", nullToEmpty(params.getQuery())));
        model.addAttribute("items", page.getContent());
        model.addAttribute("query", nullToEmpty(params.getQuery()));
        model.addAttribute("deletedView", false);
        return LIST_VIEW;
    }

    @GetMapping("/deleted")
    public String deleted(PageParams params, Model model) {
        Page<ManufacturerDto> page = manufacturerService.findDeleted(params.toPageable(SORTABLE, "name"));
        ListModel.fill(model, PATH + "/deleted", params, SORTABLE, "name", page, Map.of());
        model.addAttribute("items", page.getContent());
        model.addAttribute("query", "");
        model.addAttribute("deletedView", true);
        return LIST_VIEW;
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("manufacturer", new ManufacturerDto());
        model.addAttribute("formAction", PATH + "/add");
        return FORM_VIEW;
    }

    @PostMapping("/add")
    public String create(@Valid @ModelAttribute("manufacturer") ManufacturerDto dto, BindingResult result,
                         Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("formAction", PATH + "/add");
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            manufacturerService.create(dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.rejectValue("name", exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return FORM_VIEW;
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("manufacturer", manufacturerService.findById(id));
        model.addAttribute("formAction", PATH + "/" + id + "/edit");
        return FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("manufacturer") ManufacturerDto dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("formAction", PATH + "/" + id + "/edit");
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            manufacturerService.update(id, dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.rejectValue("name", exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return FORM_VIEW;
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        manufacturerService.delete(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.deleted");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        manufacturerService.restore(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.restored");
        return "redirect:" + PATH + "/deleted";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
