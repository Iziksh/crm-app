package com.crm.service;

import com.crm.domain.entity.Quote;
import com.crm.domain.entity.QuoteLineItem;
import com.crm.domain.entity.SalesOrder;
import com.crm.domain.entity.SalesOrderLineItem;
import com.crm.domain.enums.QuoteStatus;
import com.crm.dto.request.QuoteLineItemRequest;
import com.crm.dto.request.QuoteRequest;
import com.crm.dto.response.QuoteResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.OpportunityRepository;
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
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        SalesOrderRepository salesOrderRepository,
                        UserRepository userRepository,
                        AccountRepository accountRepository,
                        ContactRepository contactRepository,
                        OpportunityRepository opportunityRepository) {
        this.quoteRepository = quoteRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
    }

    public QuoteResponse create(QuoteRequest request, String createdByUsername) {
        Quote quote = mapToEntity(new Quote(), request);
        userRepository.findByUsername(createdByUsername).ifPresent(quote::setCreatedBy);
        quote.recalculateTotal();
        return QuoteResponse.from(quoteRepository.save(quote));
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(Long id) {
        return QuoteResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<QuoteResponse> findAll(Pageable pageable) {
        return quoteRepository.findAll(pageable).map(QuoteResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<QuoteResponse> findAll(Pageable pageable, QuoteStatus status) {
        if (status != null) {
            return quoteRepository.findByStatus(status, pageable).map(QuoteResponse::from);
        }
        return quoteRepository.findAll(pageable).map(QuoteResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(QuoteStatus status) {
        return status != null ? quoteRepository.countByStatus(status) : quoteRepository.count();
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> findByStatus(QuoteStatus status) {
        return quoteRepository.findByStatus(status).stream().map(QuoteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> findByOpportunity(Long opportunityId) {
        return quoteRepository.findByOpportunity_Id(opportunityId).stream().map(QuoteResponse::from).toList();
    }

    public QuoteResponse update(Long id, QuoteRequest request) {
        Quote quote = getOrThrow(id);
        quote.getLineItems().clear();
        mapToEntity(quote, request);
        quote.recalculateTotal();
        return QuoteResponse.from(quoteRepository.save(quote));
    }

    public void delete(Long id) {
        quoteRepository.delete(getOrThrow(id));
    }

    public QuoteResponse addLineItem(Long quoteId, QuoteLineItemRequest request) {
        Quote quote = getOrThrow(quoteId);
        QuoteLineItem item = buildLineItem(request, quote);
        quote.getLineItems().add(item);
        quote.recalculateTotal();
        return QuoteResponse.from(quoteRepository.save(quote));
    }

    public QuoteResponse removeLineItem(Long quoteId, Long lineItemId) {
        Quote quote = getOrThrow(quoteId);
        quote.getLineItems().removeIf(i -> i.getId().equals(lineItemId));
        quote.recalculateTotal();
        return QuoteResponse.from(quoteRepository.save(quote));
    }

    public SalesOrderResponse convertToOrder(Long quoteId) {
        Quote quote = getOrThrow(quoteId);
        if (quote.getStatus() != QuoteStatus.WON) {
            throw new BadRequestException("Quote must have status WON to convert to a Sales Order");
        }
        SalesOrder order = new SalesOrder();
        order.setQuote(quote);
        order.setAccount(quote.getAccount());
        order.setContact(quote.getContact());
        order.setAssignedTo(quote.getAssignedTo());
        order.setCreatedBy(quote.getCreatedBy());
        order.setCurrency(quote.getCurrency());
        order.setNotes(quote.getNotes());

        for (QuoteLineItem src : quote.getLineItems()) {
            SalesOrderLineItem dest = new SalesOrderLineItem();
            dest.setSalesOrder(order);
            dest.setProductName(src.getProductName());
            dest.setQuantity(src.getQuantity());
            dest.setUnitPrice(src.getUnitPrice());
            dest.setDiscountPct(src.getDiscountPct());
            dest.setSortOrder(src.getSortOrder());
            order.getLineItems().add(dest);
        }
        order.recalculateTotal();
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    private Quote mapToEntity(Quote quote, QuoteRequest request) {
        quote.setTitle(request.title());
        quote.setValidUntil(request.validUntil());
        quote.setCurrency(request.currency() != null ? request.currency() : "USD");
        quote.setNotes(request.notes());
        if (request.status() != null) quote.setStatus(request.status());
        if (request.assignedToId() != null) {
            quote.setAssignedTo(userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId())));
        } else {
            quote.setAssignedTo(null);
        }
        if (request.accountId() != null) {
            quote.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        } else {
            quote.setAccount(null);
        }
        if (request.contactId() != null) {
            quote.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
        } else {
            quote.setContact(null);
        }
        if (request.opportunityId() != null) {
            quote.setOpportunity(opportunityRepository.findById(request.opportunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", request.opportunityId())));
        } else {
            quote.setOpportunity(null);
        }
        if (request.lineItems() != null) {
            for (QuoteLineItemRequest itemReq : request.lineItems()) {
                quote.getLineItems().add(buildLineItem(itemReq, quote));
            }
        }
        return quote;
    }

    private QuoteLineItem buildLineItem(QuoteLineItemRequest request, Quote quote) {
        QuoteLineItem item = new QuoteLineItem();
        item.setQuote(quote);
        item.setProductName(request.productName());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDiscountPct(request.discountPct() != null ? request.discountPct() : java.math.BigDecimal.ZERO);
        item.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        item.calcLineTotal();
        return item;
    }

    private Quote getOrThrow(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", "id", id));
    }
}
