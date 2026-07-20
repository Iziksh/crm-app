package com.crm;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the dev-profile 2FA bypass (auth.login-2fa-enabled=false): valid credentials return a
 * JWT immediately, with no OTP step, for every role type. Mirrors what local `dev` runs do.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.login-2fa-enabled=false")
class LoginDevBypassIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String ME = "/api/v1/auth/me";
    private static final String PASSWORD = "Passw0rd!123";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Workspaces first: the workspace_members join table has an FK to users.
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @ParameterizedTest(name = "{0} logs in directly (no OTP) when 2FA is disabled")
    @ValueSource(strings = {
            "ROLE_SUPER_ADMIN", "ROLE_COMPANY_ADMIN", "ROLE_ADMIN",
            "ROLE_HR_MANAGER", "ROLE_SALES", "ROLE_SUPPORT", "ROLE_USER"
    })
    void loginBypassesOtpForEveryRole(String role) throws Exception {
        String username = "user_" + role.replace("ROLE_", "").toLowerCase();
        createUser(username, role);

        MvcResult result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", hasItem(role)));
    }

    private User createUser(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(u);
    }
}
