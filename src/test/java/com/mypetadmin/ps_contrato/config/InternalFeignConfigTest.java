package com.mypetadmin.ps_contrato.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalFeignConfigTest {

    private final InternalFeignConfig config = new InternalFeignConfig();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void deveRejeitarChaveInternaAusente() {
        assertThrows(IllegalStateException.class, () -> config.internalKeyInterceptor(" "));
    }

    @Test
    void devePropagarChaveInternaECorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-123");
        RequestInterceptor interceptor = config.internalKeyInterceptor("internal-secret");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertTrue(template.headers().get("X-Internal-Key").contains("internal-secret"));
        assertTrue(template.headers().get(CorrelationIdFilter.HEADER_NAME).contains("corr-123"));
    }

    @Test
    void naoDeveCriarCorrelationIdQuandoNaoExisteNoMdc() {
        RequestInterceptor interceptor = config.internalKeyInterceptor("internal-secret");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertTrue(template.headers().get("X-Internal-Key").contains("internal-secret"));
        assertTrue(!template.headers().containsKey(CorrelationIdFilter.HEADER_NAME));
    }
}
