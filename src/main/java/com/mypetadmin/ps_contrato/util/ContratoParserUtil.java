package com.mypetadmin.ps_contrato.util;

public class ContratoParserUtil {
    public static String getAno(String numeroContrato) {
        return numeroContrato.substring(0, 4); // AAAA
    }

    public static String getMes(String numeroContrato) {
        return numeroContrato.substring(4, 6); // MM
    }

    public static String getSequencia(String numeroContrato) {
        return numeroContrato.substring(6); // NNNNNN
    }
}
