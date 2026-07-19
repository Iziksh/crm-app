package com.crm.security;

import com.crm.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RoleHierarchy roleHierarchy;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                    RoleHierarchy roleHierarchy) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        // An expired/malformed token is an authentication failure, not a server error: swallow the
        // JwtException, leave the context unauthenticated, and let the chain return 401 downstream
        // instead of surfacing a 500 with a full stacktrace.
        try {
            final String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    // Expand via the role hierarchy (SUPER_ADMIN > COMPANY_ADMIN > ADMIN > ...) so JWT-based
                    // REST requests get the same effective authorities as session-based Vaadin login, which
                    // applies this same hierarchy through the AuthenticationProvider's authoritiesMapper.
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, roleHierarchy.getReachableGrantedAuthorities(userDetails.getAuthorities()));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException ex) {
            // Invalid token — proceed unauthenticated. Debug-level, since expiry is routine.
            logger.debug("Ignoring invalid JWT: " + ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
