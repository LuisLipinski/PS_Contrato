package com.mypetadmin.ps_contrato.enums;

public enum SortField {

    DATA_CRIACAO("dataCriacao"),
    NUMERO_CONTRATO("contractNumber"),
    STATUS("status.statusName"),
    EMPRESA_ID("empresaId");

    private final String field;

    SortField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
