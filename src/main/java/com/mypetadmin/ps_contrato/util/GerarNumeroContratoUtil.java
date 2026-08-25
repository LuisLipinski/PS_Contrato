package com.mypetadmin.ps_contrato.util;

public final class GerarNumeroContratoUtil {

    private GerarNumeroContratoUtil() {
    }

    public static String gerarNumeroContrato(String periodo, long sequencial) {
        if (periodo == null || !periodo.matches("\\d{6}")) {
            throw new IllegalArgumentException("Período do contrato deve estar no formato yyyyMM.");
        }
        if (sequencial < 1 || sequencial > 999999) {
            throw new IllegalArgumentException("Sequencial do contrato deve estar entre 1 e 999999.");
        }

        return periodo + String.format("%06d", sequencial);
    }
}
