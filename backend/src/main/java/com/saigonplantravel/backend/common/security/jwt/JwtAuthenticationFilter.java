package com.saigonplantravel.backend.common.security.jwt;

import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.common.security.RestAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JwtAuthenticationService jwtAuthenticationService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            JwtAuthenticationService jwtAuthenticationService,
            RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication currentAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (currentAuthentication == null) {
                AccessTokenClaims tokenClaims = jwtService.parseAccessToken(token);

                Authentication authentication = jwtAuthenticationService.createAuthentication(tokenClaims);

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

                securityContext.setAuthentication(authentication);

                SecurityContextHolder.setContext(securityContext);
            }

            filterChain.doFilter(request, response);

        } catch (InvalidAccessTokenException exception) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request, response, new BadCredentialsException(exception.getMessage(), exception));
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null) {
            return null;
        }

        boolean hasBearerPrefix = authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());

        if (!hasBearerPrefix) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
