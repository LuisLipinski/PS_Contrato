package com.mypetadmin.ps_contrato.exception;

public class PagamentoConfirmacaoInvalidaException extends RuntimeException {

    public PagamentoConfirmacaoInvalidaException(String message) {
        super(message);
    }

    public PagamentoConfirmacaoInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
