package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.model.ApplianceTranslation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplianceSpecifications {

    public static Specification<Appliance> filter(String query, Category category, Long manufacturerId, PowerType powerType) {
        return Specification.where(notDeleted())
                .and(textMatches(query))
                .and(hasCategory(category))
                .and(hasManufacturer(manufacturerId))
                .and(hasPowerType(powerType));
    }

    public static Specification<Appliance> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    private static Specification<Appliance> textMatches(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                translationMatches(root, query, cb, pattern),
                cb.like(cb.lower(root.get("model")), pattern),
                cb.like(cb.lower(root.get("manufacturer").get("name")), pattern));
    }

    private static Predicate translationMatches(Root<Appliance> root, CriteriaQuery<?> query, CriteriaBuilder cb, String pattern) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<ApplianceTranslation> translation = subquery.from(ApplianceTranslation.class);
        subquery.select(translation.<Long>get("id")).where(
                cb.equal(translation.get("applianceId"), root.get("id")),
                cb.like(cb.lower(translation.<String>get("name")), pattern));
        return cb.exists(subquery);
    }

    private static Specification<Appliance> hasCategory(Category category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private static Specification<Appliance> hasManufacturer(Long manufacturerId) {
        return manufacturerId == null ? null
                : (root, query, cb) -> cb.equal(root.get("manufacturer").get("id"), manufacturerId);
    }

    private static Specification<Appliance> hasPowerType(PowerType powerType) {
        return powerType == null ? null : (root, query, cb) -> cb.equal(root.get("powerType"), powerType);
    }
}
