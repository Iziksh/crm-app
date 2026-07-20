package com.crm;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.crm.service.OtpService;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end login through the real email-2FA gate, exercised for every role type:
 * credentials → OTP required → verify → JWT → /auth/me returns the expected role.
 *
 * <p>OtpService is mocked so the emailed code is deterministic; JavaMailSender is mocked so
 * no real mail is sent. The 2FA gate itself is left ON (default for the test profile).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginRoleIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String VERIFY = "/api/v1/auth/login/verify-otp";
    private static final String ME = "/api/v1/auth/me";
    private static final String PASSWORD = "Passw0rd!123";
    private static final String OTP = "123456";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean OtpService otpService;
    @MockBean JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        // Workspaces first: DataInitializer seeds an admin into a workspace, and the
        // workspace_members join table has an FK to users.
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // Deterministic OTP: any login stores "123456", and only that code validates.
        when(otpService.generateAndStore(anyString())).thenReturn(OTP);
        when(otpService.validate(anyString(), eq(OTP))).thenReturn(true);

        MimeMessage fakeMime = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(fakeMime);
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @ParameterizedTest(name = "{0} can log in through 2FA and /me reports the role")
    @ValueSource(strings = {
            "ROLE_SUPER_ADMIN", "ROLE_COMPANY_ADMIN", "ROLE_ADMIN",
            "ROLE_HR_MANAGER", "ROLE_SALES", "ROLE_SUPPORT", "ROLE_USER"
    })
    void loginThroughTwoFactorForEveryRole(String role) throws Exception {
        String username = usernameFor(role);
        createUser(username, role);

        // Step 1 — valid credentials do NOT yield a token yet; a code is required.
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(loginJson(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(true))
                .andExpect(jsonPath("$.maskedEmail").isNotEmpty());

        // Step 2 — the emailed code completes login and returns a JWT.
        MvcResult verified = mockMvc.perform(post(VERIFY).contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson(username, OTP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Step 3 — the token authenticates and /me reports exactly this role.
        String token = JsonPath.read(verified.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", hasItem(role)));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        createUser("bob", "ROLE_USER");
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("bob", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nobody", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongOtpIsRejected() throws Exception {
        createUser("carol", "ROLE_USER");
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(loginJson("carol", PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(post(VERIFY).contentType(MediaType.APPLICATION_JSON).content(verifyJson("carol", "000000")))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(u);
    }

    private static String usernameFor(String role) {
        return "user_" + role.replace("ROLE_", "").toLowerCase();
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String verifyJson(String username, String otp) {
        return "{\"username\":\"" + username + "\",\"otp\":\"" + otp + "\",\"trustDevice\":false}";
    }
}
