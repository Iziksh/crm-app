package com.crm.billing.service;

import com.crm.billing.dto.CustomerQuickCreateRequest;
import com.crm.billing.dto.CustomerQuickCreateResponse;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.AccountType;
import com.crm.exception.DuplicateEmailException;
import com.crm.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Thin wrapper over the existing {@code Account}/{@code AccountRepository} — the customer picker's
 * "quick create" dialog and lazy search, tenant-scoped exactly like {@code AccountService} already is.
 * No new Customer entity: Account plays that role for this addon (see Phase 26 entity-mapping note). */
@Service
@Transactional
public class CustomerQuickCreateService {

    private final AccountRepository accountRepository;
    private final BillingTenantSupport tenantSupport;

    public CustomerQuickCreateService(AccountRepository accountRepository, BillingTenantSupport tenantSupport) {
        this.accountRepository = accountRepository;
        this.tenantSupport = tenantSupport;
    }

    public CustomerQuickCreateResponse create(CustomerQuickCreateRequest request) {
        if (request.email() != null && !request.email().isBlank() && accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Account", request.email());
        }
        Workspace workspace = tenantSupport.currentWorkspaceOrThrow();

        Account account = new Account();
        account.setName(request.name());
        account.setTaxId(request.taxId());
        account.setPhone(request.phone());
        account.setEmail(request.email());
        account.setAddress(request.address());
        account.setIndustry(request.industry());
        account.setWebsite(request.website());
        account.setType(AccountType.CUSTOMER);
        account.setWorkspace(workspace);

        return CustomerQuickCreateResponse.from(accountRepository.save(account));
    }

    /** Lazy search over the current tenant's customers, for the line-item grid's customer picker. */
    @Transactional(readOnly = true)
    public List<CustomerQuickCreateResponse> search(String query) {
        Workspace workspace = tenantSupport.currentWorkspaceOrThrow();
        List<Account> matches = (query == null || query.isBlank())
                ? accountRepository.findAllByWorkspaceIds(List.of(workspace.getId()))
                : accountRepository.searchAllByWorkspaceIds(query, List.of(workspace.getId()));
        return matches.stream().map(CustomerQuickCreateResponse::from).toList();
    }
}
