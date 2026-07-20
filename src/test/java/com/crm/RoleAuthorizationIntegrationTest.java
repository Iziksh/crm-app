package com.crm;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.User;
import com.crm.repository.AccountRepository;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that each role can access only what it is authorized to see on the Users API —
 * not merely that it can log in. Runs with the 2FA bypass so tokens are obtained directly.
 *
 * <p>Fixture (two companies + a global admin):
 * <ul>
 *   <li>Company A: {@code caA} (COMPANY_ADMIN), {@code hrA} (HR_MANAGER), {@code salesA} (SALES)</li>
 *   <li>Company B: {@code userB} (USER), {@code supportB} (SUPPORT)</li>
 *   <li>Global: {@code gAdmin} (ADMIN, no account)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.login-2fa-enabled=false")
class RoleAuthorizationIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String USERS = "/api/v1/users";
    private static final String PASSWORD = "Passw0rd!123";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Account companyB;

    @BeforeEach
    void setUp() {
        // Clean slate — drop the seeded admin/VladiK so counts are exact. Workspaces first (FK).
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();

        Account companyA = accountRepository.save(named("Company A"));
        companyB = accountRepository.save(named("Company B"));

        createUser("gAdmin", "ROLE_ADMIN", null);           // global, no account
        createUser("caA", "ROLE_COMPANY_ADMIN", companyA);  // company A admin
        createUser("hrA", "ROLE_HR_MANAGER", companyA);
        createUser("salesA", "ROLE_SALES", companyA);
        createUser("userB", "ROLE_USER", companyB);
        createUser("supportB", "ROLE_SUPPORT", companyB);
    }

    @Test
    void standardUsersCannotAccessUsersList() throws Exception {
        for (String username : new String[]{"userB", "supportB", "salesA"}) {
            mockMvc.perform(get(USERS).header("Authorization", bearer(username)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void globalAdminSeesEveryUser() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", bearer("gAdmin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username",
                        containsInAnyOrder("gAdmin", "caA", "hrA", "salesA", "userB", "supportB")));
    }

    @Test
    void companyAdminSeesOnlyItsOwnCompanysUsers() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", bearer("caA")))
                .andExpect(status().isOk())
                // Only company A — never userB/supportB (company B) or the global gAdmin.
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("caA", "hrA", "salesA")));
    }

    @Test
    void companyAdminCannotEscapeScopeViaAccountIdParam() throws Exception {
        // Even explicitly asking for company B's account, a company admin still sees only its own.
        mockMvc.perform(get(USERS)
                        .param("accountId", String.valueOf(companyB.getId()))
                        .header("Authorization", bearer("caA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("caA", "hrA", "salesA")));
    }

    @Test
    void hrManagerCanReadUsersButNotCreate() throws Exception {
        // Read is allowed for the attendance pickers...
        mockMvc.perform(get(USERS).header("Authorization", bearer("hrA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems("caA", "hrA", "salesA")));

        // ...but management is ADMIN-only.
        String body = "{\"username\":\"x\",\"email\":\"x@test.com\",\"password\":\"" + PASSWORD
                + "\",\"roles\":[\"ROLE_USER\"],\"accountId\":" + companyB.getId() + "}";
        mockMvc.perform(post(USERS).header("Authorization", bearer("hrA"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void standardUserCannotCreateUsers() throws Exception {
        String body = "{\"username\":\"x\",\"email\":\"x@test.com\",\"password\":\"" + PASSWORD
                + "\",\"roles\":[\"ROLE_USER\"],\"accountId\":" + companyB.getId() + "}";
        mockMvc.perform(post(USERS).header("Authorization", bearer("userB"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Account named(String name) {
        Account a = new Account();
        a.setName(name);
        return a;
    }

    private void createUser(String username, String role, Account account) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRoles(new HashSet<>(Set.of(role)));
        u.setAccount(account);
        userRepository.save(u);
    }

    private String bearer(String username) throws Exception {
        MvcResult r = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(r.getResponse().getContentAsString(), "$.token");
        return "Bearer " + token;
    }
}
