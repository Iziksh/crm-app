package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.dto.request.RegisterRequest;
import com.crm.dto.request.UserRequest;
import com.crm.dto.response.UserResponse;
import com.crm.dto.response.UserSummaryResponse;
import com.crm.exception.BadRequestException;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<String> findEmailByUsername(String username) {
        return userRepository.findByUsername(username).map(User::getEmail);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findAll() {
        return userRepository.findAll().stream().map(UserSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, pageable).map(UserResponse::from);
        }
        return userRepository.findAll(pageable).map(UserResponse::from);
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
        return UserResponse.from(getOrThrow(id));
    }

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateEmailException(request.username());
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(
                request.password() != null && !request.password().isBlank() ? request.password() : "changeme"));
        user.setRoles(request.roles() != null && !request.roles().isEmpty() ? request.roles() : Set.of("ROLE_USER"));
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = getOrThrow(id);
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        }
        return UserResponse.from(userRepository.save(user));
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
}
