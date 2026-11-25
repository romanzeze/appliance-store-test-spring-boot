package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.ApplianceTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplianceTranslationRepository extends JpaRepository<ApplianceTranslation, Long> {

    List<ApplianceTranslation> findByApplianceIdInAndLanguage(Collection<Long> applianceIds, String language);

    Optional<ApplianceTranslation> findByApplianceIdAndLanguage(Long applianceId, String language);
}
