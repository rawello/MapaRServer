package com.rawello.maparserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request: {} {} with JWT: {}",
                    request.getMethod(), request.getRequestURI(),
                    header != null ? "present" : "absent");
        }

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (LOG.isDebugEnabled()) {
            String tokenPrefix = token.length() >= 8 ? token.substring(0, 8) : token;
            LOG.debug("JWT token extracted from request: {}...", tokenPrefix);
        }

        try {
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                LOG.debug("Setting up authentication for user: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (userDetails != null) {
                    List<String> roles = tokenProvider.getRolesFromToken(token);
                    LOG.debug("Retrieved roles from JWT token for user {}: {}", username, roles);

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    LOG.debug("Successfully authenticated user: {} with roles: {}", username, roles);
                }
            }
        } catch (Exception ex) {
            LOG.error("Cannot process authentication: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}