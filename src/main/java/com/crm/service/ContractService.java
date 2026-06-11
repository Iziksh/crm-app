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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final SalesOrderRepository salesOrderRepository;

    public ContractService(ContractRepository contractRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository,
                           ContactRepository contactRepository,
                           SalesOrderRepository salesOrderRepository) {
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.salesOrderRepository = salesOrderRepository;
    }

    public ContractResponse create(ContractRequest request, String createdByUsername) {
        Contract contract = mapToEntity(new Contract(), request);
        userRepository.findByUsername(createdByUsername).ifPresent(contract::setCreatedBy);
        return ContractResponse.from(contractRepository.save(contract));
    }

    @Transactional(readOnly = true)
    public ContractResponse findById(Long id) {
        return ContractResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> findAll(Pageable pageable) {
        return contractRepository.findAll(pageable).map(ContractResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> findAll(Pageable pageable, ContractStatus status) {
        if (status != null) {
            return contractRepository.findByStatus(status, pageable).map(ContractResponse::from);
        }
        return contractRepository.findAll(pageable).map(ContractResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(ContractStatus status) {
        return status != null ? contractRepository.countByStatus(status) : contractRepository.count();
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> findByStatus(ContractStatus status) {
        return contractRepository.findByStatus(status).stream().map(ContractResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> findExpiringWithin(int days) {
        return contractRepository.findByEndDateBefore(LocalDate.now().plusDays(days))
                .stream().map(ContractResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countExpiringWithin(int days) {
        return contractRepository.countByEndDateBefore(LocalDate.now().plusDays(days));
    }

    public ContractResponse update(Long id, ContractRequest request) {
        return ContractResponse.from(contractRepository.save(mapToEntity(getOrThrow(id), request)));
    }

    public void delete(Long id) {
        contractRepository.delete(getOrThrow(id));
    }

    private Contract mapToEntity(Contract contract, ContractRequest request) {
        contract.setTitle(request.title());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setTotalValue(request.totalValue());
        contract.setCurrency(request.currency() != null ? request.currency() : "USD");
        contract.setDescription(request.description());
        contract.setTerms(request.terms());
        if (request.status() != null) contract.setStatus(request.status());
        if (request.assignedToId() != null) {
            contract.setAssignedTo(userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId())));
        } else {
            contract.setAssignedTo(null);
        }
        if (request.accountId() != null) {
            contract.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        } else {
            contract.setAccount(null);
        }
        if (request.contactId() != null) {
            contract.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
        } else {
            contract.setContact(null);
        }
        if (request.salesOrderId() != null) {
            contract.setSalesOrder(salesOrderRepository.findById(request.salesOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", request.salesOrderId())));
        } else {
            contract.setSalesOrder(null);
        }
        return contract;
    }

    private Contract getOrThrow(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", id));
    }
}
