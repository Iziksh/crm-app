package com.crm.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final Cache<String, String> cache;
    private final int otpLength;
    private final SecureRandom random = new SecureRandom();

    public OtpService(
            @Value("${otp.expiry-minutes:5}") int expiryMinutes,
            @Value("${otp.length:6}") int otpLength) {
        this.otpLength = otpLength;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expiryMinutes, TimeUnit.MINUTES)
                .build();
    }

    public String generateAndStore(String email) {
        String otp = generateCode();
        cache.put(email.toLowerCase(), otp);
        return otp;
    }

    public boolean validate(String email, String code) {
        String stored = cache.getIfPresent(email.toLowerCase());
        if (stored != null && stored.equals(code)) {
            cache.invalidate(email.toLowerCase());
            return true;
        }
        return false;
    }

    private String generateCode() {
        int max = (int) Math.pow(10, otpLength);
        return String.format("%0" + otpLength + "d", random.nextInt(max));
    }
}
