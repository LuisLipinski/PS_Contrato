package com.mypetadmin.ps_contrato.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InternalRequestFilterTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chaveValidaDeveAutenticarComoRoleInternal() throws Exception {
        InternalRequestFilter filter = new InternalRequestFilter(INTERNAL_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/contratos");
        request.addHeader(InternalRequestFilter.INTERNAL_KEY_HEADER, INTERNAL_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("internal-service");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_INTERNAL");
        verify(chain).doFilter(request, response);
    }

    @Test
    void chaveInvalidaNaoDeveCriarAuthentication() throws Exception {
        InternalRequestFilter filter = new InternalRequestFilter(INTERNAL_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/contratos");
        request.addHeader(InternalRequestFilter.INTERNAL_KEY_HEADER, "invalid-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void chaveAusenteNaoDeveCriarAuthentication() throws Exception {
        InternalRequestFilter filter = new InternalRequestFilter(INTERNAL_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/contratos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
