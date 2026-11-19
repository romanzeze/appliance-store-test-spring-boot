package com.epam.rd.autocode.assessment.appliances.controller.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListModel {

    public static void fill(Model model, String path, PageParams params, Set<String> sortable, String defaultSort,
                            Page<?> page, Map<String, ?> filters) {
        String sort = params.sortProperty(sortable, defaultSort);
        String dir = params.direction().name().toLowerCase();
        StringBuilder filterQs = new StringBuilder();
        filters.forEach((key, value) -> {
            if (value != null && !value.toString().isBlank()) {
                filterQs.append('&').append(key).append('=')
                        .append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
            }
        });
        model.addAttribute("path", path);
        model.addAttribute("page", page);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", params.effectiveSize());
        model.addAttribute("filterQs", filterQs.toString());
        model.addAttribute("listQs", "&size=" + params.effectiveSize() + "&sort=" + sort + "&dir=" + dir + filterQs);
    }
}
