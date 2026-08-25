package com.mypetadmin.ps_contrato.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OnboardingLockService {

    private final JdbcTemplate jdbcTemplate;

    public void lock(UUID onboardingId) {
        long lockKey = onboardingId.getMostSignificantBits() ^ onboardingId.getLeastSignificantBits();
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", rs -> null, lockKey);
    }
}
