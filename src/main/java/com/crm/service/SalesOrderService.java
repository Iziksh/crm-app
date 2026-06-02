package com.crm.service;

import com.crm.domain.entity.Contract;
import com.crm.domain.entity.SalesOrder;
import com.crm.domain.entity.SalesOrderLineItem;
import com.crm.domain.enums.SalesOrderStatus;
import com.crm.dto.request.SalesOrderLineItemRequest;
import com.crm.dto.request.SalesOrderRequest;
import com.crm.dto.response.ContractResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.ContractRepository;
import com.crm.repository.QuoteRepository;
import com.crm.repository.SalesOrderRepository;
import com.crm.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final QuoteRepository quoteRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             ContractRepository contractRepository,
                             UserRepository userRepository,
                             AccountRepository accountRepository,
                             ContactRepository contactRepository,
                             QuoteRepository quoteRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.quoteRepository = quoteRepository;
    }

    public SalesOrderResponse create(SalesOrderRequest request, String createdByUsername) {
        SalesOrder order = mapToEntity(new SalesOrder(), request);
        userRepository.findByUsername(createdByUsername).ifPresent(order::setCreatedBy);
        order.recalculateTotal();
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse findById(Long id) {
        return SalesOrderResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderResponse> findAll(Pageable pageable) {
        return salesOrderRepository.findAll(pageable).map(SalesOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderResponse> findAll(Pageable pageable, SalesOrderStatus status) {
        if (status != null) {
            return salesOrderRepository.findByStatus(status, pageable).map(SalesOrderResponse::from);
        }
        return salesOrderRepository.findAll(pageable).map(SalesOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(SalesOrderStatus status) {
        return status != null ? salesOrderRepository.countByStatus(status) : salesOrderRepository.count();
    }

    @Transactional(readOnly = true)
    public List<SalesOrderResponse> findByStatus(SalesOrderStatus status) {
        return salesOrderRepository.findByStatus(status).stream().map(SalesOrderResponse::from).toList();
    }

    public SalesOrderResponse update(Long id, SalesOrderRequest request) {
        SalesOrder order = getOrThrow(id);
        order.getLineItems().clear();
        mapToEntity(order, request);
        order.recalculateTotal();
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    public void delete(Long id) {
        salesOrderRepository.delete(getOrThrow(id));
    }

    public SalesOrderResponse addLineItem(Long orderId, SalesOrderLineItemRequest request) {
        SalesOrder order = getOrThrow(orderId);
        order.getLineItems().add(buildLineItem(request, order));
        order.recalculateTotal();
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    public SalesOrderResponse removeLineItem(Long orderId, Long lineItemId) {
        SalesOrder order = getOrThrow(orderId);
        order.getLineItems().removeIf(i -> i.getId().equals(lineItemId));
        order.recalculateTotal();
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    public ContractResponse convertToContract(Long orderId) {
        SalesOrder order = getOrThrow(orderId);
        if (order.getStatus() != SalesOrderStatus.DELIVERED) {
            throw new BadRequestException("Sales Order must have status DELIVERED to convert to a Contract");
        }
        Contract contract = new Contract();
        contract.setSalesOrder(order);
        contract.setTitle(order.getOrderNumber());
        contract.setTotalValue(order.getTotalAmount());
        contract.setCurrency(order.getCurrency());
        contract.setAccount(order.getAccount());
        contract.setContact(order.getContact());
        contract.setAssignedTo(order.getAssignedTo());
        contract.setCreatedBy(order.getCreatedBy());
        return ContractResponse.from(contractRepository.save(contract));
    }

    private SalesOrder mapToEntity(SalesOrder order, SalesOrderRequest request) {
        order.setOrderDate(request.orderDate());
        order.setDeliveryDate(request.deliveryDate());
        order.setCurrency(request.currency() != null ? request.currency() : "USD");
        order.setNotes(request.notes());
        if (request.status() != null) order.setStatus(request.status());
        if (request.assignedToId() != null) {
            order.setAssignedTo(userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId())));
        } else {
            order.setAssignedTo(null);
        }
        if (request.accountId() != null) {
            order.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        } else {
            order.setAccount(null);
        }
        if (request.contactId() != null) {
            order.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
        } else {
            order.setContact(null);
        }
        if (request.quoteId() != null) {
            order.setQuote(quoteRepository.findById(request.quoteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quote", "id", request.quoteId())));
        } else {
            order.setQuote(null);
        }
        if (request.lineItems() != null) {
            for (SalesOrderLineItemRequest itemReq : request.lineItems()) {
                order.getLineItems().add(buildLineItem(itemReq, order));
            }
        }
        return order;
    }

    private SalesOrderLineItem buildLineItem(SalesOrderLineItemRequest request, SalesOrder order) {
        SalesOrderLineItem item = new SalesOrderLineItem();
        item.setSalesOrder(order);
        item.setProductName(request.productName());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDiscountPct(request.discountPct() != null ? request.discountPct() : java.math.BigDecimal.ZERO);
        item.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        item.calcLineTotal();
        return item;
    }

    private SalesOrder getOrThrow(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
    }
}
