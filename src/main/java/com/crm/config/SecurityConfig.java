package com.crm.config;

import com.crm.security.JwtAuthenticationFilter;
import com.crm.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final RoleHierarchy roleHierarchy;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService,
                           RoleHierarchy roleHierarchy) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.roleHierarchy = roleHierarchy;
    }

    /** Highest-priority chain: handles all /api/** with stateless JWT auth. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(apiCorsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/users/verify-invite").permitAll()
                .requestMatchers("/api/v1/invitations/accept").permitAll()
                // Anonymous, token-gated download for WhatsApp-shared documents (recipient is not a
                // CRM user) — must be matched before the /api/v1/billing/** SALES/ADMIN rule below.
                .requestMatchers("/api/v1/billing/public/**").permitAll()
                .requestMatchers("/api/v1/admin/users/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "COMPANY_ADMIN")
                .requestMatchers("/api/v1/leads/**", "/api/v1/opportunities/**",
                        "/api/v1/quotes/**", "/api/v1/sales-orders/**",
                        "/api/v1/contracts/**", "/api/v1/forecast/**",
                        "/api/v1/billing/**").hasAnyRole("SALES", "ADMIN")
                .requestMatchers("/api/v1/activities/**").hasAnyRole("SUPPORT", "ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Vaadin UI chain — VaadinWebSecurity handles the rest (lower priority). */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            );
        super.configure(http);
        setLoginView(http, LoginView.class);
        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/login", "POST")));
    }

    /**
     * Dev-only CORS for the React SPA (crm-web) running on the Vite dev server, which is a
     * different origin than the Spring Boot app. Not needed by Vaadin (same-origin) or by
     * non-browser API clients. Tighten allowedOrigins to the real static-asset origin in prod.
     */
    @Bean
    public CorsConfigurationSource apiCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setAuthoritiesMapper(authorities ->
                roleHierarchy.getReachableGrantedAuthorities(authorities));
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
