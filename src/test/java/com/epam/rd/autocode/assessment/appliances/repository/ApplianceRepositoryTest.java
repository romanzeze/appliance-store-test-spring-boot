package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplianceRepositoryTest {

    @Autowired
    private ApplianceRepository applianceRepository;

    @Autowired
    private EntityManager entityManager;

    private long count(String query, Category category, Long manufacturerId, PowerType powerType) {
        return applianceRepository
                .findAll(ApplianceSpecifications.filter(query, category, manufacturerId, powerType), PageRequest.of(0, 20))
                .getTotalElements();
    }

    @Test
    void withoutFiltersReturnsAllActiveAppliances() {
        assertEquals(7, count(null, null, null, null));
    }

    @Test
    void filtersByCategory() {
        assertEquals(3, count(null, Category.BIG, null, null));
        assertEquals(4, count(null, Category.SMALL, null, null));
    }

    @Test
    void filtersByTextCaseInsensitively() {
        assertEquals(2, count("VACUUM", null, null, null));
    }

    @Test
    void combinesFilters() {
        assertEquals(2, count("vacuum", Category.SMALL, null, PowerType.ACCUMULATOR));
    }

    @Test
    void sortsAndPaginates() {
        var page = applianceRepository.findAll(ApplianceSpecifications.filter(null, null, null, null),
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "price")));
        assertEquals(3, page.getContent().size());
        assertEquals("Refrigerator", page.getContent().get(0).getName());
        assertEquals(7, page.getTotalElements());
    }

    @Test
    void softDeletedApplianceIsHiddenAndCanBeRestored() {
        Appliance appliance = applianceRepository.findAll().stream()
                .filter(a -> a.getName().equals("Hair dryer")).findFirst().orElseThrow();
        applianceRepository.delete(appliance);
        applianceRepository.flush();
        entityManager.clear();

        assertEquals(6, count(null, null, null, null));
        assertEquals(1, applianceRepository.findByDeletedTrue(PageRequest.of(0, 10)).getTotalElements());
        assertTrue(applianceRepository.findByIdAndDeletedFalse(appliance.getId()).isEmpty());

        applianceRepository.restore(appliance.getId());
        assertEquals(7, count(null, null, null, null));
    }

    @Test
    void detectsActiveAppliancesOfManufacturer() {
        Appliance appliance = applianceRepository.findAll().get(0);
        assertTrue(applianceRepository.existsByManufacturerIdAndDeletedFalse(appliance.getManufacturer().getId()));
    }
}
