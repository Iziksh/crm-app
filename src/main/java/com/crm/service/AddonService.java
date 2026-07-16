package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.Addon;
import com.crm.dto.request.AddonRequest;
import com.crm.dto.response.AddonResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.AddonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AddonService {

    private final AddonRepository addonRepository;
    private final AccountRepository accountRepository;

    public AddonService(AddonRepository addonRepository, AccountRepository accountRepository) {
        this.addonRepository = addonRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AddonResponse> findByAccount(Long accountId) {
        return addonRepository.findByAccount_IdOrderByNameAsc(accountId)
                .stream().map(AddonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public boolean accountHasActiveAddon(Long accountId, String addonName) {
        if (accountId == null) return false;
        return addonRepository.findByAccount_IdOrderByNameAsc(accountId).stream()
                .anyMatch(a -> addonName.equals(a.getName())
                        && (a.getExpiryDate() == null || !a.getExpiryDate().isBefore(LocalDate.now())));
    }

    public AddonResponse create(Long accountId, AddonRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        Addon addon = new Addon();
        addon.setAccount(account);
        return AddonResponse.from(addonRepository.save(mapToEntity(addon, request)));
    }

    public AddonResponse update(Long addonId, AddonRequest request) {
        Addon addon = getOrThrow(addonId);
        return AddonResponse.from(addonRepository.save(mapToEntity(addon, request)));
    }

    public void delete(Long addonId) {
        addonRepository.delete(getOrThrow(addonId));
    }

    private Addon getOrThrow(Long id) {
        return addonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Addon", "id", id));
    }

    private Addon mapToEntity(Addon addon, AddonRequest request) {
        addon.setName(request.name());
        addon.setDescription(request.description());
        addon.setExpiryDate(request.expiryDate());
        return addon;
    }
}
