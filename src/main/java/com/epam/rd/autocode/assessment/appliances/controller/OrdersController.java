package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.controller.support.Cart;
import com.epam.rd.autocode.assessment.appliances.controller.support.FlashMessages;
import com.epam.rd.autocode.assessment.appliances.controller.support.ListModel;
import com.epam.rd.autocode.assessment.appliances.controller.support.PageParams;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceFilterDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderDto;
import com.epam.rd.autocode.assessment.appliances.dto.user.ClientDto;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRowDto;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.security.CurrentUserService;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private static final String PATH = "/orders";
    private static final String LIST_VIEW = "order/list";
    private static final String FORM_VIEW = "order/form";
    private static final String DETAILS_VIEW = "order/details";
    private static final Set<String> SORTABLE = Set.of("id", "approved");
    private static final int PICKER_SIZE = 200;

    private final OrderService orderService;
    private final ApplianceService applianceService;
    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final Cart cart;

    @InitBinder("order")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("clientId", "rows*");
        binder.setAutoGrowCollectionLimit(200);
    }

    @GetMapping
    public String list(PageParams params, @RequestParam(required = false) Boolean approved, Model model) {
        Actor actor = currentUserService.requireActor();
        Page<OrderDto> page = actor.isStaff()
                ? orderService.findAll(approved, params.toPageable(SORTABLE, "id"), actor)
                : orderService.findByClient(actor.id(), params.toPageable(SORTABLE, "id"), actor);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("approved", approved);
        ListModel.fill(model, PATH, params, SORTABLE, "id", page, filters);
        model.addAttribute("items", page.getContent());
        model.addAttribute("approved", approved);
        model.addAttribute("deletedView", false);
        return LIST_VIEW;
    }
    @GetMapping("/deleted")
    public String deleted(PageParams params, Model model) {
        Page<OrderDto> page = orderService.findDeleted(params.toPageable(SORTABLE, "id"),
                currentUserService.requireActor());
        ListModel.fill(model, PATH + "/deleted", params, SORTABLE, "id", page, Map.of());
        model.addAttribute("items", page.getContent());
        model.addAttribute("approved", null);
        model.addAttribute("deletedView", true);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long applianceId, Model model) {
        OrderDto order = new OrderDto();
        order.getRows().addAll(cart.rows());
        if (applianceId != null && order.getRows().stream().noneMatch(row -> applianceId.equals(row.getApplianceId()))) {
            order.getRows().add(OrderRowDto.builder().applianceId(applianceId).number(1L).build());
        }
        if (order.getRows().isEmpty()) {
            order.getRows().add(OrderRowDto.builder().number(1L).build());
        }
        model.addAttribute("order", order);
        return form(model, PATH + "/new", true);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("order") OrderDto dto, BindingResult result, Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return form(model, PATH + "/new", true);
        }
        try {
            OrderDto created = orderService.create(dto, currentUserService.requireActor());
            cart.clear();
            FlashMessages.success(redirectAttributes, "msg.order.created");
            return "redirect:" + PATH + "/" + created.getId();
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return form(model, PATH + "/new", true);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id, currentUserService.requireActor()));
        return DETAILS_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id, currentUserService.requireActor()));
        return form(model, PATH + "/" + id + "/edit", false);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("order") OrderDto dto,
                         BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return form(model, PATH + "/" + id + "/edit", false);
        }
        try {
            orderService.update(id, dto, currentUserService.requireActor());
        } catch (BusinessException exception) {
            result.reject(exception.getMessageKey(), exception.getArgs(), exception.getMessageKey());
            return form(model, PATH + "/" + id + "/edit", false);
        }
        FlashMessages.success(redirectAttributes, "msg.saved");
        return "redirect:" + PATH + "/" + id;
    }
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.approve(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.order.approved");
        return "redirect:" + PATH + "/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.delete(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.deleted");
        return "redirect:" + PATH;
    }
    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.restore(id, currentUserService.requireActor());
        FlashMessages.success(redirectAttributes, "msg.restored");
        return "redirect:" + PATH + "/deleted";
    }

    private String form(Model model, String action, boolean creating) {
        Actor actor = currentUserService.requireActor();
        model.addAttribute("formAction", action);
        model.addAttribute("creating", creating);
        model.addAttribute("staff", actor.isStaff());
        model.addAttribute("appliances", applianceService
                .search(new ApplianceFilterDto(), PageRequest.of(0, PICKER_SIZE, Sort.by("name"))).getContent());
        if (actor.isStaff() && creating) {
            Object order = model.getAttribute("order");
            Long clientId = order instanceof OrderDto dto ? dto.getClientId() : null;
            model.addAttribute("selectedClient", clientId == null ? null : findClient(clientId));
        }
        return FORM_VIEW;
    }

    private ClientDto findClient(Long id) {
        try {
            return clientService.findById(id);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
