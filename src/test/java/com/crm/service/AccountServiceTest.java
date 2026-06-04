package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.dto.request.AccountRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock CrmEventPublisher eventPublisher;
    @InjectMocks AccountService accountService;

    @Test
    void create_savesAccount() {
        AccountRequest req = new AccountRequest("Acme", null, null, null, null, null, null, null);
        Account saved = new Account();
        saved.setName("Acme");
        when(accountRepository.save(any())).thenReturn(saved);

        AccountResponse response = accountService.create(req);

        assertThat(response.name()).isEqualTo("Acme");
        verify(accountRepository).save(any());
    }

    @Test
    void create_throwsDuplicateEmail_whenEmailExists() {
        AccountRequest req = new AccountRequest("Acme", null, null, null, "test@acme.com", null, null, null);
        when(accountRepository.existsByEmail("test@acme.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> accountService.create(req));
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.findById(99L));
    }

    @Test
    void delete_callsRepositoryDelete() {
        Account account = new Account();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        accountService.delete(1L);

        verify(accountRepository).delete(account);
    }
}
