package com.mypetadmin.ps_contrato.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoNumeroGeneratorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ContratoNumeroGenerator generator;

    @Test
    void deveGerarNumeroComSequencialRetornadoPeloBanco() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(42L);

        String numero = generator.gerarProximoNumero();

        assertTrue(numero.matches("\\d{6}000042"));
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), anyString());
    }

    @Test
    void deveFalharQuandoBancoNaoRetornaSequencial() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(null);

        assertThrows(IllegalStateException.class, generator::gerarProximoNumero);
    }
}
