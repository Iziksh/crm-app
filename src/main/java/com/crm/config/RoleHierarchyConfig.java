package com.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * Split out from SecurityConfig: JwtAuthenticationFilter depends on RoleHierarchy (to expand
 * authorities for JWT-authenticated requests the same way session-based login does), and
 * SecurityConfig depends on JwtAuthenticationFilter — declaring this bean on SecurityConfig itself
 * would make it depend on its own instantiation.
 */
@Configuration
public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_SUPER_ADMIN > ROLE_COMPANY_ADMIN\n" +
                "ROLE_COMPANY_ADMIN > ROLE_ADMIN\n" +
                "ROLE_ADMIN > ROLE_SALES\n" +
                "ROLE_ADMIN > ROLE_SUPPORT\n" +
                "ROLE_ADMIN > ROLE_USER"
        );
    }
}
