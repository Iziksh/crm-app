package com.crm.billing.service;

import com.crm.billing.dto.LineItemRequest;
import com.crm.billing.dto.PaymentRequestCreateRequest;
import com.crm.billing.entity.DocumentLineItem;
import com.crm.billing.entity.PaymentRequest;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.exception.AlreadyConvertedException;
import com.crm.billing.exception.CrossTenantAccessException;
import com.crm.billing.exception.NotEditableException;
import com.crm.billing.repository.PaymentRequestRepository;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestServiceTest {

    @Mock PaymentRequestRepository paymentRequestRepository;
    @Mock DocumentLineItemSupport lineItemSupport;
    @Mock BillingTenantSupport tenantSupport;
    @Mock PaymentRequestNumberingService numberingService;

    private PaymentRequestService service;

    @BeforeEach
    void setUp() {
        service = new PaymentRequestService(paymentRequestRepository, lineItemSupport, tenantSupport, numberingService);
    }

    private PaymentRequestCreateRequest sampleRequest(Long accountId) {
        return new PaymentRequestCreateRequest(accountId, "ILS", LocalDate.of(2026, 7, 1), "note",
                List.of(new LineItemRequest(null, "Widget", null, BigDecimal.ONE, BigDecimal.TEN, 0)));
    }

    @Test
    void update_allowedWhileOpen() {
        PaymentRequest pr = new PaymentRequest();
        pr.setId(1L);
        pr.setStatus(PaymentRequestStatus.OPEN);
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        pr.setWorkspace(workspace);

        when(tenantSupport.isAdmin()).thenReturn(false);
        when(tenantSupport.currentUserWorkspaceIds()).thenReturn(List.of(10L));
        when(paymentRequestRepository.findByIdAndWorkspaceIds(1L, List.of(10L))).thenReturn(Optional.of(pr));

        Account account = new Account();
        account.setId(2L);
        when(tenantSupport.loadCustomer(2L, workspace)).thenReturn(account);
        when(lineItemSupport.replaceItems(any(), eq(1L), any())).thenReturn(List.of());
        when(lineItemSupport.sumLineTotals(any())).thenReturn(BigDecimal.TEN);
        when(paymentRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, sampleRequest(2L));

        assertThat(pr.getStatus()).isEqualTo(PaymentRequestStatus.OPEN);
    }

    @Test
    void update_rejected_onceConverted() {
        PaymentRequest pr = new PaymentRequest();
        pr.setId(1L);
        pr.setStatus(PaymentRequestStatus.CONVERTED);
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        pr.setWorkspace(workspace);

        when(tenantSupport.isAdmin()).thenReturn(false);
        when(tenantSupport.currentUserWorkspaceIds()).thenReturn(List.of(10L));
        when(paymentRequestRepository.findByIdAndWorkspaceIds(1L, List.of(10L))).thenReturn(Optional.of(pr));

        assertThrows(NotEditableException.class, () -> service.update(1L, sampleRequest(2L)));
    }

    @Test
    void cancel_rejected_onceConverted() {
        PaymentRequest pr = new PaymentRequest();
        pr.setId(1L);
        pr.setStatus(PaymentRequestStatus.CONVERTED);

        when(tenantSupport.isAdmin()).thenReturn(false);
        when(tenantSupport.currentUserWorkspaceIds()).thenReturn(List.of(10L));
        when(paymentRequestRepository.findByIdAndWorkspaceIds(1L, List.of(10L))).thenReturn(Optional.of(pr));

        assertThrows(AlreadyConvertedException.class, () -> service.cancel(1L));
    }

    @Test
    void getOwned_crossTenant_blocked() {
        when(tenantSupport.isAdmin()).thenReturn(false);
        when(tenantSupport.currentUserWorkspaceIds()).thenReturn(List.of(10L));
        when(paymentRequestRepository.findByIdAndWorkspaceIds(1L, List.of(10L))).thenReturn(Optional.empty());

        assertThrows(CrossTenantAccessException.class, () -> service.findById(1L));
    }

    @Test
    void getOwned_adminBypassesTenantCheck() {
        PaymentRequest pr = new PaymentRequest();
        pr.setId(1L);
        pr.setStatus(PaymentRequestStatus.OPEN);
        when(tenantSupport.isAdmin()).thenReturn(true);
        when(paymentRequestRepository.findByIdFetched(1L)).thenReturn(Optional.of(pr));
        when(lineItemSupport.find(any(), eq(1L))).thenReturn(List.<DocumentLineItem>of());

        assertThat(service.findById(1L)).isNotNull();
        verify(paymentRequestRepository, never()).findByIdAndWorkspaceIds(any(), any());
    }
}
