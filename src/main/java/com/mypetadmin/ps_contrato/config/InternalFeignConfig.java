package com.mypetadmin.ps_contrato.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    @Bean
    RequestInterceptor internalKeyInterceptor(@Value("${security.internal-key:}") String internalKey) {
        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY deve estar configurada para comunicação entre microsserviços");
        }

        return template -> template.header("X-Internal-Key", internalKey);
    }
}
