package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {

    Page<Manufacturer> findByDeletedFalse(Pageable pageable);

    List<Manufacturer> findByDeletedFalse(Sort sort);

    Page<Manufacturer> findByDeletedTrue(Pageable pageable);

    Optional<Manufacturer> findByIdAndDeletedFalse(Long id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    @Query("select m from Manufacturer m where m.deleted = false and lower(m.name) like lower(concat('%', :query, '%'))")
    Page<Manufacturer> search(@Param("query") String query, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Manufacturer m set m.deleted = false where m.id = :id")
    int restore(@Param("id") Long id);
}
