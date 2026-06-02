package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.AccountGroup;
import com.crm.dto.request.AccountGroupRequest;
import com.crm.dto.response.AccountGroupResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountGroupRepository;
import com.crm.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountGroupService {

    private final AccountGroupRepository groupRepository;
    private final AccountRepository accountRepository;

    public AccountGroupService(AccountGroupRepository groupRepository, AccountRepository accountRepository) {
        this.groupRepository = groupRepository;
        this.accountRepository = accountRepository;
    }

    public AccountGroupResponse create(AccountGroupRequest request) {
        AccountGroup group = mapToEntity(new AccountGroup(), request);
        return AccountGroupResponse.from(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public AccountGroupResponse findById(Long id) {
        return AccountGroupResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AccountGroupResponse> findAll(Pageable pageable) {
        return groupRepository.findAll(pageable).map(AccountGroupResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AccountGroupResponse> findAll(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return groupRepository.findByNameContainingIgnoreCase(search, pageable).map(AccountGroupResponse::from);
        }
        return groupRepository.findAll(pageable).map(AccountGroupResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(String search) {
        if (search != null && !search.isBlank()) {
            return groupRepository.countByNameContainingIgnoreCase(search);
        }
        return groupRepository.count();
    }

    @Transactional(readOnly = true)
    public List<AccountGroupResponse> search(String name) {
        return groupRepository.findByNameContainingIgnoreCase(name)
                .stream().map(AccountGroupResponse::from).toList();
    }

    public AccountGroupResponse update(Long id, AccountGroupRequest request) {
        AccountGroup group = getOrThrow(id);
        return AccountGroupResponse.from(groupRepository.save(mapToEntity(group, request)));
    }

    public void delete(Long id) {
        groupRepository.delete(getOrThrow(id));
    }

    public AccountGroupResponse addMember(Long groupId, Long accountId) {
        AccountGroup group = getOrThrow(groupId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        if (!group.getMembers().contains(account)) {
            group.getMembers().add(account);
        }
        return AccountGroupResponse.from(groupRepository.save(group));
    }

    public AccountGroupResponse removeMember(Long groupId, Long accountId) {
        AccountGroup group = getOrThrow(groupId);
        group.getMembers().removeIf(a -> a.getId().equals(accountId));
        return AccountGroupResponse.from(groupRepository.save(group));
    }

    private AccountGroup getOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AccountGroup", "id", id));
    }

    private AccountGroup mapToEntity(AccountGroup group, AccountGroupRequest request) {
        group.setName(request.name());
        group.setDescription(request.description());
        if (request.parentId() != null) {
            group.setParent(groupRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("AccountGroup", "id", request.parentId())));
        } else {
            group.setParent(null);
        }
        return group;
    }
}
