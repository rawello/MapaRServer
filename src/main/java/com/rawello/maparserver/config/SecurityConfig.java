package com.rawello.maparserver.config;

import com.rawello.maparserver.security.JwtAuthenticationFilter;
import com.rawello.maparserver.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        private static final String[] PUBLIC_URLS = {
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/login.html",
                        "/register.html",
                        "/index.html",
                        "/api/auth/validate"
        };

        // URL patterns
        private static final String[] GET_PERMITTED_PATHS = {
                        "/buildings/**",
                        "/api/buildings/**",
                        "/api/nodes/**",
                        "/api/users/is-anonymous",
                        "/nodes/**"
        };

        private static final String[] OWNER_PATHS = {
                        "/buildings/{buildingId}/nodes/{nodeId}",
                        "/buildings/{buildingId}/nodes/**",
                        "/buildings/{buildingId}/**",
                        "/buildings/**",
                        "/api/buildings/{id}/**",
                        "/api/buildings/{id}/nodes/**"
        };

        private static final String ROLE_OWNER = "ROLE_OWNER";
        private static final String ROLE_USER = "ROLE_USER";

        private final CustomUserDetailsService userDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(CustomUserDetailsService userDetailsService,
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.userDetailsService = userDetailsService;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.csrf(AbstractHttpConfigurer::disable)
                                .cors(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .anonymous(anonymous -> anonymous.authorities("ROLE_ANONYMOUS"))
                                .authorizeHttpRequests(auth -> {
                                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                        .requestMatchers(PUBLIC_URLS).permitAll()
                                                        // Разрешаем все GET запросы для всех путей
                                                        .requestMatchers(HttpMethod.GET, GET_PERMITTED_PATHS)
                                                        .permitAll()

                                                        // PUT запросы для владельцев
                                                        .requestMatchers(HttpMethod.PUT, OWNER_PATHS)
                                                        .hasAuthority(ROLE_OWNER)

                                                        // DELETE запросы для владельцев
                                                        .requestMatchers(HttpMethod.DELETE, OWNER_PATHS)
                                                        .hasAuthority(ROLE_OWNER)

                                                        // POST запросы для пользователей и владельцев
                                                        .requestMatchers(HttpMethod.POST,
                                                                        "/buildings/**",
                                                                        "/api/buildings/**",
                                                                        "/api/nodes/**")
                                                        .hasAnyAuthority(ROLE_USER, ROLE_OWNER)

                                                        // Специальные пути для пользователей
                                                        .requestMatchers("/api/users/favorites/**")
                                                        .hasAnyAuthority(ROLE_USER, ROLE_OWNER)
                                                        // Пути только для админа
                                                        .requestMatchers("/api/users").hasAuthority("ROLE_ADMIN")

                                                        .anyRequest().authenticated();

                                        logger.debug("Security matchers configured - GET requests are now permitted for anonymous users");
                                })
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                logger.debug("Security configuration completed");
                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManagerBean(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);
                authenticationManagerBuilder
                                .userDetailsService(userDetailsService)
                                .passwordEncoder(passwordEncoder());
                AuthenticationManager authenticationManager = authenticationManagerBuilder.build();
                logger.debug("Authentication manager configured with custom UserDetailsService");
                return authenticationManager;
        }
}