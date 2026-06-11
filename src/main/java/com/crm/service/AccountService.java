package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.dto.request.AccountRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CrmEventPublisher eventPublisher;
    private final WorkspaceContext workspaceContext;

    public AccountService(AccountRepository accountRepository,
                          CrmEventPublisher eventPublisher,
                          WorkspaceContext workspaceContext) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
        this.workspaceContext = workspaceContext;
    }

    public AccountResponse create(AccountRequest request) {
        if (request.email() != null && accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Account account = mapToEntity(new Account(), request);
        workspaceContext.currentUserPrimaryWorkspace().ifPresent(account::setWorkspace);
        AccountResponse response = AccountResponse.from(accountRepository.save(account));
        eventPublisher.publishCreated("ACCOUNT", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        return AccountResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable) {
        if (workspaceContext.isAdmin()) {
            return accountRepository.findAll(pageable).map(AccountResponse::from);
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        return accountRepository.findByWorkspace_IdIn(wsIds, pageable).map(AccountResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable, String search) {
        if (workspaceContext.isAdmin()) {
            if (search != null && !search.isBlank()) {
                return accountRepository.findByNameContainingIgnoreCase(search, pageable).map(AccountResponse::from);
            }
            return accountRepository.findAll(pageable).map(AccountResponse::from);
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        if (search != null && !search.isBlank()) {
            return accountRepository.searchByWorkspaceIds(search, wsIds, pageable).map(AccountResponse::from);
        }
        return accountRepository.findByWorkspace_IdIn(wsIds, pageable).map(AccountResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(String search) {
        if (workspaceContext.isAdmin()) {
            if (search != null && !search.isBlank()) {
                return accountRepository.countByNameContainingIgnoreCase(search);
            }
            return accountRepository.count();
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return 0;
        if (search != null && !search.isBlank()) {
            return accountRepository.countSearchByWorkspaceIds(search, wsIds);
        }
        return accountRepository.countByWorkspace_IdIn(wsIds);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> search(String name) {
        if (workspaceContext.isAdmin()) {
            return accountRepository.findByNameContainingIgnoreCase(name)
                    .stream().map(AccountResponse::from).toList();
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return List.of();
        return accountRepository.searchAllByWorkspaceIds(name, wsIds)
                .stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAllForExport(String search) {
        if (workspaceContext.isAdmin()) {
            if (search != null && !search.isBlank()) {
                return accountRepository.findByNameContainingIgnoreCase(search,
                        org.springframework.data.domain.Pageable.unpaged()).getContent()
                        .stream().map(AccountResponse::from).toList();
            }
            return accountRepository.findAll().stream().map(AccountResponse::from).toList();
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return List.of();
        if (search != null && !search.isBlank()) {
            return accountRepository.searchAllByWorkspaceIds(search, wsIds)
                    .stream().map(AccountResponse::from).toList();
        }
        return accountRepository.findAllByWorkspaceIds(wsIds).stream().map(AccountResponse::from).toList();
    }

    public AccountResponse update(Long id, AccountRequest request) {
        Account account = getOrThrow(id);
        if (request.email() != null && !request.email().equals(account.getEmail())
                && accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        AccountResponse response = AccountResponse.from(accountRepository.save(mapToEntity(account, request)));
        eventPublisher.publishUpdated("ACCOUNT", id);
        return response;
    }

    public void delete(Long id) {
        accountRepository.delete(getOrThrow(id));
        eventPublisher.publishDeleted("ACCOUNT", id);
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
