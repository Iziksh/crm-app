package com.crm;

import com.crm.domain.entity.Invitation;
import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.dto.request.InviteRequest;
import com.crm.exception.InvitationInvalidException;
import com.crm.repository.InvitationRepository;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.crm.service.InvitationService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvitationFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired InvitationService invitationService;
    @Autowired InvitationRepository invitationRepository;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PlatformTransactionManager txManager;

    @MockBean JavaMailSender mailSender;

    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        testWorkspace = new Workspace();
        testWorkspace.setName("Test Workspace");
        testWorkspace = workspaceRepository.save(testWorkspace);

        User admin = new User();
        admin.setUsername("testadmin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("adminpassword123!"));
        admin.setRoles(Set.of("ROLE_ADMIN"));
        userRepository.save(admin);

        MimeMessage fakeMime = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(fakeMime);
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void anonymousRegisterIsRejected() throws Exception {
        // Endpoint was removed — expect 404; security might return 403/401 first, but never 201
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"hacker","email":"hacker@evil.com","password":"password1234"}
                                """))
                .andExpect(status().is(org.hamcrest.Matchers.not(201)));
    }

    @Test
    void validInviteCanBeAcceptedOnce() {
        String rawToken = invitationService.createInvitation(
                new InviteRequest("invitee@example.com", "ROLE_USER", testWorkspace.getId()),
                "testadmin"
        );

        invitationService.acceptInvitation(rawToken, "newuser", "securepassword123!");

        User created = userRepository.findByEmail("invitee@example.com").orElseThrow();
        assertThat(created.getRoles()).containsExactly("ROLE_USER");
        assertThat(created.getUsername()).isEqualTo("newuser");
        assertThat(created.isEnabled()).isTrue();

        // Load members in a transaction to avoid LazyInitializationException
        boolean isMember = new TransactionTemplate(txManager).execute(status ->
                workspaceRepository.findById(testWorkspace.getId()).orElseThrow()
                        .getMembers().stream().anyMatch(u -> u.getId().equals(created.getId()))
        );
        assertThat(isMember).isTrue();

        Invitation inv = invitationRepository.findAll().stream()
                .filter(i -> i.getEmail().equals("invitee@example.com"))
                .findFirst().orElseThrow();
        assertThat(inv.getAcceptedAt()).isNotNull();
    }

    @Test
    void reuseOfAcceptedTokenFails() {
        String rawToken = invitationService.createInvitation(
                new InviteRequest("second@example.com", "ROLE_USER", testWorkspace.getId()),
                "testadmin"
        );

        invitationService.acceptInvitation(rawToken, "seconduser", "securepassword456!");

        assertThatThrownBy(() ->
                invitationService.acceptInvitation(rawToken, "anotheruser", "securepassword789!")
        ).isInstanceOf(InvitationInvalidException.class);
    }

    @Test
    void expiredTokenFails() {
        String rawToken = "expiredtoken-" + System.currentTimeMillis();

        Invitation expired = new Invitation();
        expired.setTokenHash(invitationService.sha256(rawToken));
        expired.setEmail("expired@example.com");
        expired.setRole("ROLE_USER");
        expired.setWorkspace(testWorkspace);
        expired.setExpiresAt(LocalDateTime.now().minusHours(1));
        invitationRepository.save(expired);

        assertThatThrownBy(() ->
                invitationService.acceptInvitation(rawToken, "expireduser", "password123456!")
        ).isInstanceOf(InvitationInvalidException.class);
    }

    @Test
    void createdUserHasCorrectRoleAndWorkspace() {
        String rawToken = invitationService.createInvitation(
                new InviteRequest("sales@example.com", "ROLE_SALES", testWorkspace.getId()),
                "testadmin"
        );

        invitationService.acceptInvitation(rawToken, "salesuser", "securepassword123!");

        User created = userRepository.findByEmail("sales@example.com").orElseThrow();
        assertThat(created.getRoles()).containsExactly("ROLE_SALES");

        boolean isMember = new TransactionTemplate(txManager).execute(status ->
                workspaceRepository.findById(testWorkspace.getId()).orElseThrow()
                        .getMembers().stream().anyMatch(u -> u.getId().equals(created.getId()))
        );
        assertThat(isMember).isTrue();
    }
}
