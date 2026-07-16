package com.crm.billing.service;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * ASSUMPTION: no existing WhatsApp/SMS sender was found anywhere in this codebase, and the prompt
 * says to fall back to a {@code wa.me} deep-link rather than building a full WhatsApp Business API
 * client. A real attachment cannot be pushed through a {@code wa.me} link — only the Business API
 * can do that — so the human still has to attach a downloaded PDF manually inside WhatsApp after
 * this link opens a prefilled chat; this matches the prompt's explicit instruction not to build
 * that API client here.
 *
 * ASSUMPTION: local Israeli numbers (leading "0") are normalized to the +972 country code, since no
 * real spec for accepted input formats was provided.
 */
@Service
public class WhatsAppLinkSender implements MessageChannelSender {

    @Override
    public String buildDeepLink(String phoneNumber, String message) {
        String digits = phoneNumber.replaceAll("[^0-9+]", "");
        String international = digits.startsWith("0") ? "972" + digits.substring(1) : digits.replace("+", "");
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + international + "?text=" + encodedMessage;
    }
}
