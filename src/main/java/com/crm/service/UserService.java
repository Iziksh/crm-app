package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.User;
import com.crm.dto.request.RegisterRequest;
import com.crm.dto.request.UserRequest;
import com.crm.dto.response.UserResponse;
import com.crm.dto.response.UserSummaryResponse;
import com.crm.exception.BadRequestException;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String admin2Email;

    public UserService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String adminEmail,
                       @Value("${app.admin2.email:}") String admin2Email) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.admin2Email = admin2Email;
    }

    @Transactional(readOnly = true)
    public Optional<String> findEmailByUsername(String username) {
        return userRepository.findByUsername(username).map(User::getEmail);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getAccountIdByUsername(String username) {
        return userRepository.findByUsernameWithAccount(username)
                .map(u -> u.getAccount() == null ? null : u.getAccount().getId());
    }

    /**
     * Returns the real inbox address for OTP and device trust.
     * Skips internal placeholders ({@code username@crm.internal}) created when
     * an admin email is reclaimed, and falls back to configured admin addresses.
     */
    @Transactional(readOnly = true)
    public Optional<String> findDeliverableEmailByUsername(String username) {
        return userRepository.findByUsername(username).flatMap(user -> {
            String stored = user.getEmail();
            if (isDeliverable(stored)) {
                return Optional.of(stored);
            }
            String configured = configuredEmailForUsername(username);
            if (isDeliverable(configured)) {
                return Optional.of(configured);
            }
            return Optional.empty();
        });
    }

    public static boolean isPlaceholderEmail(String email) {
        return email != null && email.endsWith("@crm.internal");
    }

    private static boolean isDeliverable(String email) {
        return email != null && !email.isBlank() && !isPlaceholderEmail(email);
    }

    private String configuredEmailForUsername(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return adminEmail;
        }
        if ("VladiK".equalsIgnoreCase(username)) {
            return admin2Email;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findAll() {
        return userRepository.findAll().stream().map(UserSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable, String search) {
        return findAll(pageable, search, null);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable, String search, Long accountId) {
        Page<User> page = userRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (accountId != null) predicates.add(cb.equal(root.get("account").get("id"), accountId));
            if (search != null && !search.isBlank()) {
                String q = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), q),
                        cb.like(cb.lower(root.get("email")), q)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable);
        Map<Long, String> managerNames = resolveManagerNames(page.getContent());
        Map<Long, String> accountNames = resolveAccountNames(page.getContent());
        return page.map(u -> UserResponse.from(u,
                managerNames.get(u.getManagerId()),
                u.getAccount() != null ? accountNames.get(u.getAccount().getId()) : null));
    }

    @Transactional(readOnly = true)
    public long count(String search) {
        if (search != null && !search.isBlank()) {
            return userRepository.countByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        }
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = getOrThrow(id);
        return UserResponse.from(user, resolveManagerName(user.getManagerId()));
    }

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateEmailException(request.username());
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        if (request.managerId() != null && !userRepository.existsById(request.managerId())) {
            throw new ResourceNotFoundException("User", "id", request.managerId());
        }
        Set<String> roles = request.roles() != null && !request.roles().isEmpty()
                ? request.roles() : Set.of("ROLE_USER");
        // Global admins stay accountless; everyone else — including COMPANY_ADMIN, scoped to its
        // own company — must belong to an account.
        Account account = null;
        if (!isGlobalAdmin(roles)) {
            if (request.accountId() == null) {
                throw new BadRequestException("An account must be assigned when creating a user");
            }
            account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId()));
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(
                request.password() != null && !request.password().isBlank() ? request.password() : "changeme"));
        user.setRoles(roles);
        user.setManagerId(request.managerId());
        user.setAccount(account);
        return UserResponse.from(userRepository.save(user),
                resolveManagerName(request.managerId()), account != null ? account.getName() : null);
    }

    /**
     * Global admins are platform-wide and stay accountless: ROLE_ADMIN and ROLE_SUPER_ADMIN.
     * COMPANY_ADMIN is deliberately excluded — it administers one company and is account-scoped
     * like an ordinary user, so it must have an account.
     */
    private boolean isGlobalAdmin(Set<String> roles) {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN"));
    }

    public void resetPassword(Long id, String newPassword) {
        User user = getOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void setAccount(Long userId, Long accountId) {
        User user = getOrThrow(userId);
        if (accountId == null) {
            user.setAccount(null);
        } else {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
            user.setAccount(account);
        }
        userRepository.save(user);
    }

    public void updateProfile(Long id, String newUsername, String newEmail) {
        User user = getOrThrow(id);
        if (newUsername != null && !newUsername.isBlank()) {
            user.setUsername(newUsername.trim());
        }
        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail.trim().toLowerCase());
        }
        userRepository.save(user);
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = getOrThrow(id);
        if (!request.username().equals(user.getUsername()) && userRepository.existsByUsername(request.username())) {
            throw new DuplicateEmailException(request.username());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        }
        if (request.managerId() != null && request.managerId().equals(id)) {
            throw new BadRequestException("A user cannot be their own manager");
        }
        if (request.managerId() != null && !userRepository.existsById(request.managerId())) {
            throw new ResourceNotFoundException("User", "id", request.managerId());
        }
        user.setManagerId(request.managerId());
        if (isGlobalAdmin(user.getRoles())) {
            // Global admins never carry an account, even if one was previously set or is sent.
            user.setAccount(null);
        } else if (request.accountId() != null) {
            // Reassignment is allowed, but an existing assignment is never cleared by omission —
            // users predating mandatory assignment can still be edited without picking an account.
            user.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        }
        User saved = userRepository.save(user);
        return UserResponse.from(saved, resolveManagerName(request.managerId()),
                saved.getAccount() != null ? saved.getAccount().getName() : null);
    }

    public UserResponse toggleEnabled(Long id) {
        User user = getOrThrow(id);
        user.setEnabled(!user.isEnabled());
        return UserResponse.from(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = getOrThrow(id);
        if (user.getRoles().contains("ROLE_ADMIN")
                && userRepository.countByRolesContaining("ROLE_ADMIN") <= 1) {
            throw new BadRequestException("Cannot delete the last admin user");
        }
        userRepository.delete(user);
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateEmailException(request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    // ── MANAGER HIERARCHY (RBAC) ──────────────────────────────────────────────

    public UserResponse setManager(Long userId, Long managerId) {
        User user = getOrThrow(userId);
        if (managerId != null) {
            if (managerId.equals(userId)) {
                throw new BadRequestException("A user cannot be their own manager");
            }
            if (!userRepository.existsById(managerId)) {
                throw new ResourceNotFoundException("User", "id", managerId);
            }
        }
        user.setManagerId(managerId);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<Long> directReportIds(Long managerId) {
        return userRepository.findByManagerId(managerId).stream().map(User::getId).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findDirectReports(Long managerId) {
        return userRepository.findByManagerId(managerId).stream()
                .map(u -> UserResponse.from(u, null))
                .toList();
    }

    /** True if {@code employeeId}'s direct manager is {@code managerId}. */
    @Transactional(readOnly = true)
    public boolean isManagerOf(Long managerId, Long employeeId) {
        if (managerId == null || employeeId == null) return false;
        return userRepository.findById(employeeId)
                .map(u -> managerId.equals(u.getManagerId()))
                .orElse(false);
    }

    private String resolveManagerName(Long managerId) {
        if (managerId == null) return null;
        return userRepository.findById(managerId).map(User::getUsername).orElse(null);
    }

    /**
     * Batch-loads account names for a page of users. Reading {@code getAccount().getId()} only
     * touches the proxy's identifier, so this stays at one extra query rather than one per row.
     */
    private Map<Long, String> resolveAccountNames(List<User> users) {
        Set<Long> accountIds = users.stream()
                .map(User::getAccount).filter(Objects::nonNull)
                .map(Account::getId).collect(Collectors.toSet());
        if (accountIds.isEmpty()) return new java.util.HashMap<>();
        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));
    }

    private Map<Long, String> resolveManagerNames(List<User> users) {
        Set<Long> managerIds = users.stream()
                .map(User::getManagerId).filter(Objects::nonNull).collect(Collectors.toSet());
        // Mutable HashMap, not Map.of(): callers look up by a possibly-null managerId, and
        // Map.of()'s .get(null) throws NPE (immutable maps reject null keys) instead of
        // returning null like a normal map would.
        if (managerIds.isEmpty()) return new java.util.HashMap<>();
        return userRepository.findAllById(managerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
