package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.dto.request.AccountRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse create(AccountRequest request) {
        if (request.email() != null && accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Account account = mapToEntity(new Account(), request);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        return AccountResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return accountRepository.findByNameContainingIgnoreCase(search, pageable).map(AccountResponse::from);
        }
        return accountRepository.findAll(pageable).map(AccountResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(String search) {
        if (search != null && !search.isBlank()) {
            return accountRepository.countByNameContainingIgnoreCase(search);
        }
        return accountRepository.count();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> search(String name) {
        return accountRepository.findByNameContainingIgnoreCase(name)
                .stream().map(AccountResponse::from).toList();
    }

    public AccountResponse update(Long id, AccountRequest request) {
        Account account = getOrThrow(id);
        if (request.email() != null && !request.email().equals(account.getEmail())
                && accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        return AccountResponse.from(accountRepository.save(mapToEntity(account, request)));
    }

    public void delete(Long id) {
        accountRepository.delete(getOrThrow(id));
    }

    private Account getOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
    }

    private Account mapToEntity(Account account, AccountRequest request) {
        account.setName(request.name());
        account.setIndustry(request.industry());
        account.setWebsite(request.website());
        account.setPhone(request.phone());
        account.setEmail(request.email());
        account.setAddress(request.address());
        account.setType(request.type());
        account.setNotes(request.notes());
        return account;
    }
}
