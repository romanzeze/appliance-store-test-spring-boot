package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplianceInOrderRepository extends JpaRepository<OrderRow, Long> {

    @Query("select r from Orders o join o.orderRowSet r join fetch r.appliance where o.id = :orderId")
    List<OrderRow> findByOrderId(@Param("orderId") Long orderId);

    boolean existsByApplianceId(Long applianceId);
}
