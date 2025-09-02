package com.mypetadmin.ps_contrato.util;

import com.mypetadmin.ps_contrato.model.Contrato;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GerarNumeroContratoUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private GerarNumeroContratoUtil() {

    }
    public static String gerarNumeroContrato(long sequencial) {
        LocalDate hoje = LocalDate.now();
        String prefixo = hoje.format(FORMATTER);
        String sequencialFormatado = String.format("%06d", sequencial);
        return prefixo + sequencialFormatado;
    }
}
