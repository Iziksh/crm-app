package com.crm.dto.response;

public record LoginResponse(
        boolean otpRequired,
        String token,
        String type,
        String username,
        String email,
        String deviceToken,
        String maskedEmail
) {
    public static LoginResponse otpRequired(String username, String maskedEmail) {
        return new LoginResponse(true, null, null, username, null, null, maskedEmail);
    }

    public static LoginResponse success(String token, String username, String email, String deviceToken) {
        return new LoginResponse(false, token, "Bearer", username, email, deviceToken, null);
    }
}
