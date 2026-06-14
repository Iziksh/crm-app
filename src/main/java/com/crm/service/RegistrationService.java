package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.exception.DuplicateEmailException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class RegistrationService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    public RegistrationService(UserRepository userRepository,
                                WorkspaceRepository workspaceRepository,
                                PasswordEncoder passwordEncoder,
                                OtpService otpService,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    public void startRegistration(String username, String email, String rawPassword,
                                   String companyName, Locale locale) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateEmailException(username);
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(com.crm.domain.enums.UserStatus.INVITED); // activated after OTP
        user.setRoles(new java.util.HashSet<>(Set.of("ROLE_COMPANY_ADMIN"))); // founder becomes company admin
        user = userRepository.save(user);

        Workspace workspace = new Workspace();
        workspace.setName(companyName.trim());
        workspace.setCreatedBy(user);
        workspace.getMembers().add(user);
        workspace = workspaceRepository.save(workspace);

        user.setWorkspaceId(workspace.getId());
        userRepository.save(user);

        String otp = otpService.generateAndStore(normalizedEmail);
        emailService.sendOtp(normalizedEmail, otp, locale);
    }

    public User completeRegistration(String email, String otp) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (!otpService.validate(normalizedEmail, otp)) {
            return null;
        }
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("User not found after OTP validation"));
        user.setStatus(com.crm.domain.enums.UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}