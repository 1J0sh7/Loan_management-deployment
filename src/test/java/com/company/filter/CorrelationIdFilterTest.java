package com.company.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {
    @Test
    void preservesIncomingCorrelationIdAndCleansMdc() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("request-id");
        doAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo("request-id");
            return null;
        }).when(chain).doFilter(request, response);

        new CorrelationIdFilter().doFilter(request, response, chain);

        verify(response).setHeader("X-Correlation-Id", "request-id");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("");
        new CorrelationIdFilter().doFilter(request, response, chain);
        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(chain).doFilter(request, response);
    }
}
