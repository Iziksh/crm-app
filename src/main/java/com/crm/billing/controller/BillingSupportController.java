package com.crm.billing.controller;

import com.crm.billing.dto.CustomerQuickCreateRequest;
import com.crm.billing.dto.CustomerQuickCreateResponse;
import com.crm.billing.dto.ShareRequest;
import com.crm.billing.dto.TotalsBreakdown;
import com.crm.billing.dto.TotalsPreviewRequest;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.service.CustomerQuickCreateService;
import com.crm.billing.service.DocumentShareService;
import com.crm.billing.service.TotalsCalculator;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Cross-cutting billing endpoints shared by both document families: live totals preview, the
 * customer quick-create/search picker, PDF rendering, and sharing (email/WhatsApp). */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingSupportController {

    private final TotalsCalculator totalsCalculator;
    private final CustomerQuickCreateService customerQuickCreateService;
    private final DocumentShareService shareService;

    public BillingSupportController(TotalsCalculator totalsCalculator,
                                     CustomerQuickCreateService customerQuickCreateService,
                                     DocumentShareService shareService) {
        this.totalsCalculator = totalsCalculator;
        this.customerQuickCreateService = customerQuickCreateService;
        this.shareService = shareService;
    }

    @PostMapping("/totals-preview")
    public ResponseEntity<TotalsBreakdown> previewTotals(@Valid @RequestBody TotalsPreviewRequest request) {
        return ResponseEntity.ok(totalsCalculator.calculate(request.lineItems(), request.discountType(),
                request.discountValue(), request.vatType(), request.roundingMode()));
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerQuickCreateResponse> createCustomer(@Valid @RequestBody CustomerQuickCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerQuickCreateService.create(request));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerQuickCreateResponse>> searchCustomers(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(customerQuickCreateService.search(q));
    }

    @GetMapping("/{ownerType}/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable DocumentOwnerType ownerType, @PathVariable Long id,
                                       @RequestParam(defaultValue = "false") boolean download) {
        byte[] pdf = shareService.renderPdf(ownerType, id);
        String disposition = (download ? "attachment" : "inline") + "; filename=\"document-" + id + ".pdf\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/share")
    public ResponseEntity<?> share(@Valid @RequestBody ShareRequest request) throws MessagingException {
        if (request.channel() == ShareRequest.ShareChannel.WHATSAPP) {
            String link = shareService.buildWhatsAppLink(request.ownerType(), request.documentId(), request.target());
            return ResponseEntity.ok(Map.of("whatsappLink", link));
        }
        shareService.share(request);
        return ResponseEntity.ok().build();
    }
}
