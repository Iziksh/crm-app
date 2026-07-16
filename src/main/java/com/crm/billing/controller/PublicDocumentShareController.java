package com.crm.billing.controller;

import com.crm.billing.entity.DocumentShareLink;
import com.crm.billing.service.DocumentShareLinkService;
import com.crm.billing.service.DocumentShareService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated public download endpoint for WhatsApp-shared documents — the recipient is not a
 * CRM user, so this can't sit behind the normal JWT-protected {@code /api/v1/billing/**} rule (see
 * {@code SecurityConfig}, which explicitly permits this one path). Access is instead gated entirely
 * by possession of a valid, unexpired share token. */
@RestController
@RequestMapping("/api/v1/billing/public")
public class PublicDocumentShareController {

    private final DocumentShareLinkService shareLinkService;
    private final DocumentShareService shareService;

    public PublicDocumentShareController(DocumentShareLinkService shareLinkService, DocumentShareService shareService) {
        this.shareLinkService = shareLinkService;
        this.shareService = shareService;
    }

    @GetMapping("/{token}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String token) {
        DocumentShareLink link = shareLinkService.resolve(token);
        byte[] pdf = shareService.renderPdfForShareLink(link);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document-" + link.getOwnerId() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
