package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClientRepositoryTest {

    private static final String EMAIL = "lebron.james@example.com";

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsClientByEmailIgnoringCase() {
        assertTrue(clientRepository.findByEmailIgnoreCaseAndDeletedFalse("LeBron.James@Example.com").isPresent());
    }

    @Test
    void searchMatchesNameOrEmailAndPaginates() {
        var page = clientRepository.search("james", PageRequest.of(0, 10, Sort.by("name")));
        assertEquals(1, page.getTotalElements());
        assertEquals("LeBron James", page.getContent().get(0).getName());
    }

    @Test
    void paginationSplitsResults() {
        var page = clientRepository.findByDeletedFalse(PageRequest.of(0, 2, Sort.by("name")));
        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }

    @Test
    void deleteIsSoftAndRestoreBringsClientBack() {
        Client client = clientRepository.findByEmailIgnoreCaseAndDeletedFalse(EMAIL).orElseThrow();
        clientRepository.delete(client);
        clientRepository.flush();
        entityManager.clear();

        assertTrue(clientRepository.findByEmailIgnoreCaseAndDeletedFalse(EMAIL).isEmpty());
        assertEquals(1, clientRepository.findByDeletedTrue(PageRequest.of(0, 10)).getTotalElements());
        assertEquals(4, clientRepository.findByDeletedFalse(PageRequest.of(0, 10)).getTotalElements());

        assertEquals(1, clientRepository.restore(client.getId()));
        assertTrue(clientRepository.findByEmailIgnoreCaseAndDeletedFalse(EMAIL).isPresent());
    }

    @Test
    void hardDeleteRemovesRowAndKeepsOrderWithoutClient() {
        Client client = clientRepository.findByEmailIgnoreCaseAndDeletedFalse(EMAIL).orElseThrow();
        Orders order = new Orders();
        order.setClient(client);
        order.setApproved(false);
        Long orderId = ordersRepository.saveAndFlush(order).getId();
        entityManager.clear();

        assertEquals(1, clientRepository.hardDeleteById(client.getId()));
        entityManager.clear();

        assertTrue(clientRepository.findById(client.getId()).isEmpty());
        assertNull(ordersRepository.findById(orderId).orElseThrow().getClient());
    }
}
