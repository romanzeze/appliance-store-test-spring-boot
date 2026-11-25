package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.controller.support.ListModel;
import com.epam.rd.autocode.assessment.appliances.controller.support.PageParams;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeCreatedDto;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.support.AccountStatusFilter;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private static final String PATH = "/employees";
    private static final String LIST_VIEW = "employee/list";
    private static final String FORM_VIEW = "employee/form";
    private static final String CREDENTIALS_VIEW = "employee/credentials";
    private static final Set<String> SORTABLE = Set.of("id", "name", "email", "department", "role", "status");
    private static final List<Role> STAFF_ROLES = List.of(Role.EMPLOYEE, Role.ADMIN);

    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    @InitBinder("employee")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "department", "role");
    }

    @ModelAttribute("employee")
    EmployeeDto employee(@PathVariable(required = false) Long id) {
        return EmployeeDto.builder().id(id).role(Role.EMPLOYEE).build();
    }

    @GetMapping
    public String list(PageParams params, Model model) {
        Page<EmployeeDto> page = employeeService.findAll(params.getQuery(), params.getStatus(), params.toPageable(SORTABLE, "name"));
        String query = params.getQuery() == null ? "" : params.getQuery();
        ListModel.fill(model, PATH, params, SORTABLE, "name", page,
                Map.of("query", query, "status", AccountStatusFilter.normalize(params.getStatus())));
        model.addAttribute("items", page.getContent());
        model.addAttribute("query", query);
        model.addAttribute("statuses", AccountStatusFilter.VALUES);
        model.addAttribute("status", AccountStatusFilter.normalize(params.getStatus()));
        model.addAttribute("roles", STAFF_ROLES);
        model.addAttribute("currentId", currentUserService.requireCurrent().getId());
        model.addAttribute("deletedView", false);
        return LIST_VIEW;
    }

    @GetMapping("/deleted")
    public String deleted(PageParams params, Model model) {
        Page<EmployeeDto> page = employeeService.findDeleted(params.toPageable(SORTABLE, "name"));
        ListModel.fill(model, PATH + "/deleted", params, SORTABLE, "name", page, Map.of());
        model.addAttribute("items", page.getContent());
        model.addAttribute("query", "");
        model.addAttribute("roles", STAFF_ROLES);
        model.addAttribute("currentId", currentUserService.requireCurrent().getId());
        model.addAttribute("deletedView", true);
        return LIST_VIEW;
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("employee", EmployeeDto.builder().role(Role.EMPLOYEE).build());
        return form(model, PATH + "/add", true);
    }

    @PostMapping("/add")
    public String create(@Valid @ModelAttribute("employee") EmployeeDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return form(model, PATH + "/add", true);
        }
        try {
            EmployeeCreatedDto created = employeeService.create(dto, currentUserService.requireActor());
            model.addAttribute("account", created.getEmployee());
            model.addAttribute("temporaryPassword", created.getTemporaryPassword());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return form(model, PATH + "/add", true);
        }
        return CREDENTIALS_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.findById(id));
        return form(model, PATH + "/" + id + "/edit", false);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("employee") EmployeeDto dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return form(model, PATH + "/" + id + "/edit", false);
        }
        try {
            employeeService.update(id, dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return form(model, PATH + "/" + id + "/edit", false);
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, RedirectAttributes redirectAttributes) {
        employeeService.changeRole(id, role, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.role.changed");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, Model model) {
        String temporaryPassword = employeeService.resetPassword(id, currentUserService.requireActor());
        model.addAttribute("account", employeeService.findById(id));
        model.addAttribute("temporaryPassword", temporaryPassword);
        return CREDENTIALS_VIEW;
    }

    @PostMapping("/{id}/block")
    public String block(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.block(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.blocked");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/unblock")
    public String unblock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.unblock(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.unblocked");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.delete(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.deleted");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.restore(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.restored");
        return "redirect:" + PATH + "/deleted";
    }


    private String form(Model model, String action, boolean creating) {
        model.addAttribute("formAction", action);
        model.addAttribute("creating", creating);
        model.addAttribute("roles", STAFF_ROLES);
        return FORM_VIEW;
    }
}
