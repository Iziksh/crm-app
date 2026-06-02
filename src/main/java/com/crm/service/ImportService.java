package com.crm.service;

import com.crm.domain.enums.AccountType;
import com.crm.dto.request.AccountRequest;
import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.ImportResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ImportService {

    private final ContactService contactService;
    private final AccountService accountService;

    public ImportService(ContactService contactService, AccountService accountService) {
        this.contactService = contactService;
        this.accountService = accountService;
    }

    /** CSV columns: first_name, last_name, email, phone, job_title, department */
    public ImportResultResponse importContacts(InputStream csv) {
        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // skip header
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] cols = parseCsvLine(line);
                if (cols.length < 3) { errors.add("Row " + row + ": need at least first_name, last_name, email"); skipped++; continue; }
                String firstName = cols[0].trim();
                String lastName  = cols[1].trim();
                String email     = cols[2].trim();
                if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
                    errors.add("Row " + row + ": first_name, last_name, email are required"); skipped++; continue;
                }
                try {
                    contactService.create(new ContactRequest(
                            firstName, lastName, email,
                            cols.length > 3 ? cols[3].trim() : null,
                            cols.length > 4 ? cols[4].trim() : null,
                            cols.length > 5 ? cols[5].trim() : null,
                            null, null, null));
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + row + ": " + e.getMessage()); skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("Parse error: " + e.getMessage());
        }
        return new ImportResultResponse(imported, skipped, errors);
    }

    /** CSV columns: name, industry, website, phone, email, type */
    public ImportResultResponse importAccounts(InputStream csv) {
        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // skip header
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] cols = parseCsvLine(line);
                if (cols.length < 1) { errors.add("Row " + row + ": need at least name"); skipped++; continue; }
                String name = cols[0].trim();
                if (name.isBlank()) { errors.add("Row " + row + ": name is required"); skipped++; continue; }
                AccountType type = null;
                if (cols.length > 5 && !cols[5].trim().isBlank()) {
                    try { type = AccountType.valueOf(cols[5].trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                try {
                    accountService.create(new AccountRequest(
                            name,
                            cols.length > 1 ? cols[1].trim() : null,
                            cols.length > 2 ? cols[2].trim() : null,
                            cols.length > 3 ? cols[3].trim() : null,
                            cols.length > 4 ? cols[4].trim() : null,
                            null, type, null));
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + row + ": " + e.getMessage()); skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("Parse error: " + e.getMessage());
        }
        return new ImportResultResponse(imported, skipped, errors);
    }

    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                tokens.add(current.toString()); current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString());
        return tokens.toArray(String[]::new);
    }
}
