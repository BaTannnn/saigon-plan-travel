package com.saigonplantravel.backend.common.security;

import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.common.security.jwt.JwtAuthenticationFilter;
import com.saigonplantravel.backend.common.security.jwt.JwtProperties;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            JwtAuthenticationService jwtAuthenticationService,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            @Value("${app.security.cors.allowed-origin}")
            String allowedOrigin
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        jwtAuthenticationService,
                        authenticationEntryPoint
                );

        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource(allowedOrigin)
                ))
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                )
                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/places/**",
                                "/api/v1/categories",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/me"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(
            String allowedOrigin
    ) {
        String normalizedOrigin = allowedOrigin.trim();
        if (normalizedOrigin.isEmpty()
                || normalizedOrigin.contains("*")) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must be an explicit origin"
            );
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(normalizedOrigin));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
