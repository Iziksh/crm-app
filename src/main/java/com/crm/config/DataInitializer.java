package com.crm.config;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           WorkspaceRepository workspaceRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin;
        if (!userRepository.existsByUsername("admin")) {
            admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@crm.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));
            admin = userRepository.save(admin);
        } else {
            admin = userRepository.findByUsername("admin").orElseThrow();
        }

        if (workspaceRepository.count() == 0) {
            Workspace defaultWs = new Workspace();
            defaultWs.setName("Default");
            defaultWs.setDescription("Default workspace");
            defaultWs.setCreatedBy(admin);
            defaultWs.getMembers().add(admin);
            workspaceRepository.save(defaultWs);
        }
    }
}
