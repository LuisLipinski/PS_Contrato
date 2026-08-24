package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.util.GerarNumeroContratoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContratoNumeroGenerator {

    private static final DateTimeFormatter PERIODO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final JdbcTemplate jdbcTemplate;

    public String gerarProximoNumero() {
        String periodo = LocalDate.now().format(PERIODO_FORMATTER);

        Long sequencial = jdbcTemplate.queryForObject(
                """
                INSERT INTO contrato_numero_sequencia (periodo, ultimo_valor)
                VALUES (?, 1)
                ON CONFLICT (periodo)
                DO UPDATE SET ultimo_valor = contrato_numero_sequencia.ultimo_valor + 1
                RETURNING ultimo_valor
                """,
                Long.class,
                periodo
        );

        if (sequencial == null) {
            throw new IllegalStateException("Não foi possível gerar o próximo sequencial do contrato.");
        }

        log.debug("contract.number generated period={} sequence={}", periodo, sequencial);
        return GerarNumeroContratoUtil.gerarNumeroContrato(periodo, sequencial);
    }
}
