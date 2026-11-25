package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.controller.support.ListModel;
import com.epam.rd.autocode.assessment.appliances.controller.support.PageParams;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.support.AccountStatusFilter;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@PreAuthorize("hasRole('EMPLOYEE')")
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private static final String PATH = "/clients";
    private static final String LIST_VIEW = "client/list";
    private static final String FORM_VIEW = "client/form";
    private static final int SEARCH_PAGE_SIZE = 4;
    private static final Set<String> SORTABLE = Set.of("id", "name", "email", "status");

    private final ClientService clientService;
    private final CurrentUserService currentUserService;

    @InitBinder("client")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "card");
    }

    @ModelAttribute("client")
    ClientDto client(@PathVariable(required = false) Long id) {
        return ClientDto.builder().id(id).build();
    }

    @GetMapping
    public String list(PageParams params, Model model) {
        Page<ClientDto> page = clientService.findAll(params.getQuery(), params.getStatus(), params.toPageable(SORTABLE, "name"));
        String query = params.getQuery() == null ? "" : params.getQuery();
        ListModel.fill(model, PATH, params, SORTABLE, "name", page,
                Map.of("query", query, "status", AccountStatusFilter.normalize(params.getStatus())));
        model.addAttribute("items", page.getContent());
        model.addAttribute("query", query);
        model.addAttribute("statuses", AccountStatusFilter.VALUES);
        model.addAttribute("status", AccountStatusFilter.normalize(params.getStatus()));
        model.addAttribute("deletedView", false);
        return LIST_VIEW;
    }

    @GetMapping("/search")
    @ResponseBody
    public ClientSearchResult search(@RequestParam(defaultValue = "") String query,
                                     @RequestParam(defaultValue = "0") int page) {
        Page<ClientDto> result = clientService.findAll(query, null,
                PageRequest.of(Math.max(page, 0), SEARCH_PAGE_SIZE, Sort.by("name")));
        List<ClientOption> items = result.getContent().stream()
                .map(client -> new ClientOption(client.getId(), client.getName(), client.getEmail()))
                .toList();
        return new ClientSearchResult(items, result.getNumber(), result.getTotalPages());
    }

    public record ClientOption(Long id, String name, String email) {
    }

    public record ClientSearchResult(List<ClientOption> items, int page, int totalPages) {
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleted(PageParams params, Model model) {
        Page<ClientDto> page = clientService.findDeleted(params.toPageable(SORTABLE, "name"));
        ListModel.fill(model, PATH + "/deleted", params, SORTABLE, "name", page, Map.of());
        model.addAttribute("items", page.getContent());
        model.addAttribute("query", "");
        model.addAttribute("deletedView", true);
        return LIST_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.findById(id));
        model.addAttribute("formAction", PATH + "/" + id + "/edit");
        return FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("client") ClientDto dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("formAction", PATH + "/" + id + "/edit");
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            clientService.update(id, dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return FORM_VIEW;
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/block")
    public String block(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clientService.block(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.blocked");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/unblock")
    public String unblock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clientService.unblock(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.unblocked");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/delete")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clientService.deactivate(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.deleted");
        return "redirect:" + PATH;
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clientService.restore(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.restored");
        return "redirect:" + PATH + "/deleted";
    }
    @PostMapping("/{id}/erase")
    @PreAuthorize("hasRole('ADMIN')")
    public String erase(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clientService.eraseAllData(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.erased");
        return "redirect:" + PATH;
    }
}
