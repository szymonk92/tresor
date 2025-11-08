package io.tresor.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Tresor API.
 *
 * Security features:
 * - CORS enabled for frontend
 * - Security headers (XSS, Frame Options, etc.)
 * - Stateless session management (JWT ready)
 * - H2 console access (development only)
 *
 * For MVP: Permissive (allow all requests)
 * For Production: Add JWT authentication
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API (using JWT tokens)
            .csrf(csrf -> csrf.disable())

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session (no session cookies)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Security headers
            .headers(headers -> headers
                .xssProtection(xss -> xss.disable()) // Modern browsers use CSP
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                                    "frame-ancestors 'none'; " +
                                    "form-action 'self';"))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)) // 1 year
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/messages/health",
                    "/h2-console/**",
                    "/error"
                ).permitAll()

                // For MVP: Allow all API requests
                // TODO: Add JWT authentication for production
                .requestMatchers("/api/**").permitAll()

                // Everything else: deny
                .anyRequest().authenticated()
            );

        // Allow H2 console in frames (development only)
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow frontend origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React dev server
            "http://localhost:5173",  // Vite dev server
            "https://tresor.app",     // Production
            "https://www.tresor.app"  // Production www
        ));

        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));

        // Allow common headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-User-Id",
            "X-Request-Id"
        ));

        // Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Request-Id"
        ));

        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
