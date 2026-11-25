package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findByDeletedFalse(Pageable pageable);

    Page<Employee> findByDeletedTrue(Pageable pageable);

    Optional<Employee> findByIdAndDeletedFalse(Long id);

    Optional<Employee> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    long countByRoleAndDeletedFalseAndBlockedFalse(Role role);

    @Query("select e from Employee e where e.deleted = false and "
            + "(lower(e.name) like lower(concat('%', :query, '%')) "
            + "or lower(e.email) like lower(concat('%', :query, '%')) "
            + "or lower(e.department) like lower(concat('%', :query, '%')))")
    Page<Employee> search(@Param("query") String query, Pageable pageable);

    @Query("select e from Employee e where e.deleted = false and "
            + "(lower(e.name) like lower(concat('%', :query, '%')) "
            + "or lower(e.email) like lower(concat('%', :query, '%')) "
            + "or lower(e.department) like lower(concat('%', :query, '%'))) and "
            + "(:status = '' or (:status = 'BLOCKED' and e.blocked = true) or (:status = 'MUST_CHANGE' and e.blocked = false and e.mustChangePassword = true) or (:status = 'ACTIVE' and e.blocked = false and e.mustChangePassword = false))")
    Page<Employee> filter(@Param("query") String query, @Param("status") String status, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Employee e set e.deleted = false where e.id = :id")
    int restore(@Param("id") Long id);
}
