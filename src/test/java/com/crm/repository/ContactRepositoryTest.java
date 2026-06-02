package com.crm.repository;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.Contact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ContactRepositoryTest {

    @Autowired ContactRepository contactRepository;
    @Autowired AccountRepository accountRepository;

    @Test
    void findByEmail_returnsContact_whenExists() {
        Contact contact = new Contact();
        contact.setFirstName("Jane");
        contact.setLastName("Doe");
        contact.setEmail("jane@example.com");
        contactRepository.save(contact);

        assertThat(contactRepository.findByEmail("jane@example.com")).isPresent();
    }

    @Test
    void existsByEmail_returnsFalse_whenMissing() {
        assertThat(contactRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findByAccount_Id_returnsContacts() {
        Account account = new Account();
        account.setName("Acme");
        account = accountRepository.save(account);

        Contact c1 = new Contact();
        c1.setFirstName("A");
        c1.setLastName("B");
        c1.setEmail("a@acme.com");
        c1.setAccount(account);

        Contact c2 = new Contact();
        c2.setFirstName("C");
        c2.setLastName("D");
        c2.setEmail("c@acme.com");
        c2.setAccount(account);

        contactRepository.save(c1);
        contactRepository.save(c2);

        List<Contact> result = contactRepository.findByAccount_Id(account.getId());
        assertThat(result).hasSize(2);
    }
}
