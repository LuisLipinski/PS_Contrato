package com.mypetadmin.ps_contrato.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GerarNumeroContratoUtilTest {

    @Test
    void deveGerarNumeroComPeriodoESequencialFormatado() {
        assertEquals("202608000042", GerarNumeroContratoUtil.gerarNumeroContrato("202608", 42));
    }

    @Test
    void deveRejeitarPeriodoNuloOuForaDoFormato() {
        assertThrows(IllegalArgumentException.class,
                () -> GerarNumeroContratoUtil.gerarNumeroContrato(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GerarNumeroContratoUtil.gerarNumeroContrato("20268", 1));
        assertThrows(IllegalArgumentException.class,
                () -> GerarNumeroContratoUtil.gerarNumeroContrato("2026AA", 1));
    }

    @Test
    void deveRejeitarSequencialForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class,
                () -> GerarNumeroContratoUtil.gerarNumeroContrato("202608", 0));
        assertThrows(IllegalArgumentException.class,
                () -> GerarNumeroContratoUtil.gerarNumeroContrato("202608", 1_000_000));
    }
}
