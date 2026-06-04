package com.crm.service;

import com.crm.domain.entity.Contract;
import com.crm.domain.enums.ContractStatus;
import com.crm.dto.request.ContractRequest;
import com.crm.dto.response.ContractResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.ContractRepository;
import com.crm.repository.SalesOrderRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock ContactRepository contactRepository;
    @Mock SalesOrderRepository salesOrderRepository;

    @InjectMocks ContractService contractService;

    @Test
    void create_savesContract_andReturnsResponse() {
        ContractRequest req = new ContractRequest(
                "Annual Support", ContractStatus.ACTIVE,
                LocalDate.now(), LocalDate.now().plusYears(1),
                null, "USD", null, null, null, null, null, null);
        Contract saved = new Contract();
        saved.setTitle("Annual Support");
        saved.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.save(any())).thenReturn(saved);

        ContractResponse response = contractService.create(req, "admin");

        assertThat(response.title()).isEqualTo("Annual Support");
        verify(contractRepository).save(any());
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contractService.findById(99L));
    }

    @Test
    void delete_callsRepositoryDelete() {
        Contract contract = new Contract();
        contract.setTitle("Old Contract");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        contractService.delete(1L);

        verify(contractRepository).delete(contract);
    }

    @Test
    void findByStatus_delegatesToRepository() {
        when(contractRepository.findByStatus(ContractStatus.ACTIVE)).thenReturn(List.of());

        List<ContractResponse> result = contractService.findByStatus(ContractStatus.ACTIVE);

        assertThat(result).isEmpty();
        verify(contractRepository).findByStatus(ContractStatus.ACTIVE);
    }

    @Test
    void findExpiringWithin_delegatesToRepository() {
        // findExpiringWithin calls findByEndDateBefore(LocalDate.now().plusDays(days))
        when(contractRepository.findByEndDateBefore(any())).thenReturn(List.of());

        List<ContractResponse> result = contractService.findExpiringWithin(30);

        assertThat(result).isEmpty();
        verify(contractRepository).findByEndDateBefore(any());
    }

    @Test
    void update_savesChanges() {
        Contract contract = new Contract();
        contract.setTitle("Old Title");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        Contract updated = new Contract();
        updated.setTitle("New Title");
        updated.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.save(any())).thenReturn(updated);

        ContractResponse response = contractService.update(1L,
                new ContractRequest("New Title", ContractStatus.ACTIVE, null, null,
                        null, null, null, null, null, null, null, null));

        assertThat(response.title()).isEqualTo("New Title");
    }
}
