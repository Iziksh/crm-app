package com.crm.billing.service;

/**
 * Abstraction for sending an outbound message on a non-email channel. The prompt explicitly asks
 * not to build a full WhatsApp Business API client here, so the only implementation
 * ({@link WhatsAppLinkSender}) returns a {@code wa.me} deep link with pre-filled text rather than
 * sending anything itself — the caller opens the link (e.g. in a new browser tab).
 */
public interface MessageChannelSender {
    /** Returns a link/URI for the caller to open, rather than transmitting anything server-side. */
    String buildDeepLink(String phoneNumber, String message);
}
