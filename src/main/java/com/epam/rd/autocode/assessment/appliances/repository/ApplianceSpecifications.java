package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
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
                cb.like(cb.lower(root.get("model")), pattern));
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
