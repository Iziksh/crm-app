package com.crm.dto.response;

public record AuthResponse(String token, String type, String username, String email) {
    public AuthResponse(String token, String username, String email) {
        this(token, "Bearer", username, email);
    }
}
