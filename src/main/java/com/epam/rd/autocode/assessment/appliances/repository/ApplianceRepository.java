package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApplianceRepository extends JpaRepository<Appliance, Long>, JpaSpecificationExecutor<Appliance> {

    @Override
    @EntityGraph(attributePaths = "manufacturer")
    Page<Appliance> findAll(Specification<Appliance> specification, Pageable pageable);

    @EntityGraph(attributePaths = "manufacturer")
    Page<Appliance> findByDeletedTrue(Pageable pageable);

    @EntityGraph(attributePaths = "manufacturer")
    Optional<Appliance> findByIdAndDeletedFalse(Long id);

    boolean existsByManufacturerIdAndDeletedFalse(Long manufacturerId);

    @Modifying(clearAutomatically = true)
    @Query("update Appliance a set a.deleted = false where a.id = :id")
    int restore(@Param("id") Long id);
}
