package com.crm.billing.service;

import com.crm.billing.exception.CrossTenantAccessException;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.service.WorkspaceContext;
import org.springframework.stereotype.Component;

/** Tenant-scoping helpers shared by every billing service — mirrors how {@code AccountService}
 * already scopes reads/writes via {@code WorkspaceContext}, so the addon introduces no second
 * tenancy model. */
@Component
public class BillingTenantSupport {

    private final WorkspaceContext workspaceContext;
    private final AccountRepository accountRepository;

    public BillingTenantSupport(WorkspaceContext workspaceContext, AccountRepository accountRepository) {
        this.workspaceContext = workspaceContext;
        this.accountRepository = accountRepository;
    }

    /** The workspace new billing documents are created under. Admin/super-admin users have no
     * primary workspace and cannot author billing documents themselves (they administer, they don't
     * bill) — this is consistent with how AccountService.create() behaves for the same users. */
    public Workspace currentWorkspaceOrThrow() {
        return workspaceContext.currentUserPrimaryWorkspace()
                .orElseThrow(() -> new IllegalStateException(
                        "No workspace context for the current user — billing documents require a workspace"));
    }

    public boolean isAdmin() {
        return workspaceContext.isAdmin();
    }

    public java.util.List<Long> currentUserWorkspaceIds() {
        return workspaceContext.currentUserWorkspaceIds();
    }

    /** Loads the customer Account and verifies it belongs to the given workspace — the same rule
     * that would otherwise let one tenant bill another tenant's customer record. */
    public Account loadCustomer(Long accountId, Workspace workspace) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        if (account.getWorkspace() == null || !account.getWorkspace().getId().equals(workspace.getId())) {
            throw new CrossTenantAccessException("Customer account not accessible from this workspace");
        }
        return account;
    }
}
