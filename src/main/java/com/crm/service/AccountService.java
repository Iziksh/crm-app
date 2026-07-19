package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.Addon;
import com.crm.dto.request.AccountRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.AddonRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final AddonRepository addonRepository;
    private final CrmEventPublisher eventPublisher;
    private final WorkspaceContext workspaceContext;

    public AccountService(AccountRepository accountRepository,
                          AddonRepository addonRepository,
                          CrmEventPublisher eventPublisher,
                          WorkspaceContext workspaceContext) {
        this.accountRepository = accountRepository;
        this.addonRepository = addonRepository;
        this.eventPublisher = eventPublisher;
        this.workspaceContext = workspaceContext;
    }

    public AccountResponse create(AccountRequest request) {
        if (request.email() != null && accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Account", request.email());
        }
        Account account = mapToEntity(new Account(), request);
        workspaceContext.currentUserPrimaryWorkspace().ifPresent(account::setWorkspace);
        AccountResponse response = AccountResponse.from(accountRepository.save(account));
        eventPublisher.publishCreated("ACCOUNT", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        Account account = getOrThrow(id);
        return AccountResponse.from(account, addonRepository.findByAccount_IdOrderByNameAsc(id));
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable) {
        if (workspaceContext.isAdmin()) {
            return withAddons(accountRepository.findAll(pageable));
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        return withAddons(accountRepository.findByWorkspace_IdIn(wsIds, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable, String search) {
        if (workspaceContext.isAdmin()) {
            if (search != null && !search.isBlank()) {
                return withAddons(accountRepository.findByNameContainingIgnoreCase(search, pageable));
            }
            return withAddons(accountRepository.findAll(pageable));
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        if (search != null && !search.isBlank()) {
            return withAddons(accountRepository.searchByWorkspaceIds(search, wsIds, pageable));
        }
        return withAddons(accountRepository.findByWorkspace_IdIn(wsIds, pageable));
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
            return withAddons(accountRepository.findByNameContainingIgnoreCase(name));
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return List.of();
        return withAddons(accountRepository.searchAllByWorkspaceIds(name, wsIds));
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
            throw new DuplicateEmailException("Account", request.email());
        }
        AccountResponse response = AccountResponse.from(accountRepository.save(mapToEntity(account, request)),
                addonRepository.findByAccount_IdOrderByNameAsc(id));
        eventPublisher.publishUpdated("ACCOUNT", id);
        return response;
    }

    public void delete(Long id) {
        accountRepository.delete(getOrThrow(id));
        eventPublisher.publishDeleted("ACCOUNT", id);
    }

    /**
     * List query honouring both scoping axes. Unlike the other entities the selected account
     * matches on the row's own id rather than an {@code account} association.
     */
    @Transactional(readOnly = true)
    public Page<AccountResponse> findAllScoped(Pageable pageable, String search, Long accountId) {
        boolean isAdmin = workspaceContext.isAdmin();
        List<Long> wsIds = isAdmin ? List.of() : workspaceContext.currentUserWorkspaceIds();
        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdmin) {
                predicates.add(wsIds.isEmpty()
                        ? cb.disjunction()
                        : root.get("workspace").get("id").in(wsIds));
            }
            if (accountId != null) predicates.add(cb.equal(root.get("id"), accountId));
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return withAddons(accountRepository.findAll(spec, pageable));
    }

    /** Loads every account's addons in one query so the list view avoids an N+1. */
    private List<AccountResponse> withAddons(List<Account> accounts) {
        if (accounts.isEmpty()) return List.of();
        List<Long> ids = accounts.stream().map(Account::getId).toList();
        Map<Long, List<Addon>> byAccountId = addonRepository.findByAccount_IdInOrderByNameAsc(ids)
                .stream().collect(Collectors.groupingBy(addon -> addon.getAccount().getId()));
        return accounts.stream()
                .map(account -> AccountResponse.from(account, byAccountId.getOrDefault(account.getId(), List.of())))
                .toList();
    }

    private Page<AccountResponse> withAddons(Page<Account> page) {
        return new PageImpl<>(withAddons(page.getContent()), page.getPageable(), page.getTotalElements());
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
