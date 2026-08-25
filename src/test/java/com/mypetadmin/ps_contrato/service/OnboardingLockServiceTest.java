package com.mypetadmin.ps_contrato.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OnboardingLockServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OnboardingLockService lockService;

    @Test
    @SuppressWarnings("unchecked")
    void deveUsarUuidParaGerarChaveDoAdvisoryLock() {
        UUID onboardingId = UUID.fromString("12345678-1234-5678-90ab-cdef12345678");
        long expectedKey = onboardingId.getMostSignificantBits() ^ onboardingId.getLeastSignificantBits();

        lockService.lock(onboardingId);

        verify(jdbcTemplate).query(
                eq("SELECT pg_advisory_xact_lock(?)"),
                any(ResultSetExtractor.class),
                eq(expectedKey)
        );
    }
}
