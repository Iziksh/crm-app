package com.crm.controller;

import com.crm.domain.entity.User;
import com.crm.dto.request.LoginRequest;
import com.crm.dto.request.RegisterRequest;
import com.crm.dto.request.ResendLoginOtpRequest;
import com.crm.dto.request.ResendOtpRequest;
import com.crm.dto.request.SignupRequest;
import com.crm.dto.request.VerifyLoginOtpRequest;
import com.crm.dto.request.VerifySignupRequest;
import com.crm.dto.response.AuthResponse;
import com.crm.dto.response.LoginResponse;
import com.crm.dto.response.MeResponse;
import com.crm.domain.entity.Account;
import com.crm.repository.AccountRepository;
import com.crm.exception.InvalidOtpException;
import com.crm.exception.NoDeliverableEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.service.DeviceTrustService;
import com.crm.service.EmailService;
import com.crm.service.JwtService;
import com.crm.service.LocaleService;
import com.crm.service.OtpService;
import com.crm.service.RegistrationService;
import com.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RegistrationService registrationService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final DeviceTrustService deviceTrustService;
    private final AccountRepository accountRepository;

    public AuthController(UserService userService, JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           RegistrationService registrationService,
                           OtpService otpService, EmailService emailService,
                           DeviceTrustService deviceTrustService,
                           AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.registrationService = registrationService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.deviceTrustService = deviceTrustService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        String token = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getUsername(), user.getEmail()));
    }

    /**
     * Authenticates credentials, then applies the same email-based 2FA gate as the Vaadin login:
     * a deliverable email is required, and unless the caller presents a valid {@code deviceToken}
     * (previously issued by {@code /login/verify-otp}), a one-time code is emailed and the client
     * must complete {@code /login/verify-otp} before receiving a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = (User) userDetails;

        String email = userService.findDeliverableEmailByUsername(user.getUsername())
                .orElseThrow(() -> new NoDeliverableEmailException(user.getUsername()));

        if (request.deviceToken() != null && deviceTrustService.isTokenValid(request.deviceToken(), email)) {
            String token = jwtService.generateToken(userDetails);
            return ResponseEntity.ok(LoginResponse.success(token, user.getUsername(), user.getEmail(), null));
        }

        String otp = otpService.generateAndStore(email);
        emailService.sendOtp(email, otp, Locale.ENGLISH);
        return ResponseEntity.ok(LoginResponse.otpRequired(user.getUsername(), maskEmail(email)));
    }

    /** Completes the login 2FA step: validates the emailed code and issues a JWT. */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<LoginResponse> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest request) {
        User user = userService.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.username()));
        String email = userService.findDeliverableEmailByUsername(request.username())
                .orElseThrow(() -> new NoDeliverableEmailException(request.username()));

        if (!otpService.validate(email, request.otp())) {
            throw new InvalidOtpException();
        }

        String token = jwtService.generateToken(user);
        String deviceToken = request.trustDevice() ? deviceTrustService.createTrustToken(email) : null;
        return ResponseEntity.ok(LoginResponse.success(token, user.getUsername(), user.getEmail(), deviceToken));
    }

    /** Re-sends the login 2FA code to a user mid-verification. */
    @PostMapping("/login/resend-otp")
    public ResponseEntity<Map<String, String>> resendLoginOtp(@Valid @RequestBody ResendLoginOtpRequest request) {
        String email = userService.findDeliverableEmailByUsername(request.username())
                .orElseThrow(() -> new NoDeliverableEmailException(request.username()));
        String otp = otpService.generateAndStore(email);
        emailService.sendOtp(email, otp, Locale.ENGLISH);
        return ResponseEntity.accepted().body(Map.of("message", "Verification code sent"));
    }

    /** Self-service company signup: creates a COMPANY_ADMIN + workspace, sends an OTP to verify the email. */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest request) {
        registrationService.startRegistration(
                request.username(), request.email(), request.password(), request.company(), Locale.ENGLISH);
        return ResponseEntity.accepted().body(Map.of("message", "Verification code sent"));
    }

    /** Re-sends the signup verification code to a pending (not-yet-activated) email. */
    @PostMapping("/signup/resend")
    public ResponseEntity<Map<String, String>> resendSignupOtp(@Valid @RequestBody ResendOtpRequest request) {
        registrationService.resendOtp(request.email(), Locale.ENGLISH);
        return ResponseEntity.accepted().body(Map.of("message", "Verification code sent"));
    }

    /** Completes signup: validates the OTP and returns a JWT for the newly-activated account. */
    @PostMapping("/signup/verify")
    public ResponseEntity<AuthResponse> verifySignup(@Valid @RequestBody VerifySignupRequest request) {
        User user = registrationService.completeRegistration(request.email(), request.otp());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail()));
    }

    /** The currently-authenticated user's own profile (id, roles, workspace) — used by clients that
     * only hold a username/email from the login response but need the numeric id for other calls. */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal User user) {
        String accountName = user.getAccount() == null ? null
                : accountRepository.findById(user.getAccount().getId()).map(Account::getName).orElse(null);
        return ResponseEntity.ok(MeResponse.from(user, accountName));
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return local.length() <= 2
                ? local.charAt(0) + "***" + domain
                : local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }
}
