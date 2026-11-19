package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Page<Client> findByDeletedFalse(Pageable pageable);

    Page<Client> findByDeletedTrue(Pageable pageable);

    Optional<Client> findByIdAndDeletedFalse(Long id);

    Optional<Client> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    @Query("select c from Client c where c.deleted = false and "
            + "(lower(c.name) like lower(concat('%', :query, '%')) or lower(c.email) like lower(concat('%', :query, '%')))")
    Page<Client> search(@Param("query") String query, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Client c set c.deleted = false where c.id = :id")
    int restore(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM client WHERE id = :id", nativeQuery = true)
    int hardDeleteById(@Param("id") Long id);
}
