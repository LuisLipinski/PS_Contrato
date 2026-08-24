package com.mypetadmin.ps_contrato.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalRequestFilter extends OncePerRequestFilter {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final String expectedKey;

    public InternalRequestFilter(@Value("${security.internal-key:}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

        if (SecurityContextHolder.getContext().getAuthentication() == null && isValid(providedKey)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "internal-service",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValid(String providedKey) {
        if (expectedKey == null || expectedKey.isBlank() || providedKey == null || providedKey.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
