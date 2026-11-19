package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByDeletedTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByApprovedAndDeletedFalse(Boolean approved, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByClientIdAndDeletedFalse(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee", "orderRowSet", "orderRowSet.appliance"})
    Optional<Orders> findWithRowsByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"client", "employee", "orderRowSet", "orderRowSet.appliance"})
    Optional<Orders> findWithRowsByIdAndClientIdAndDeletedFalse(Long id, Long clientId);

    boolean existsByIdAndClientIdAndDeletedFalse(Long id, Long clientId);

    @Modifying(clearAutomatically = true)
    @Query("update Orders o set o.deleted = false where o.id = :id")
    int restore(@Param("id") Long id);
}
