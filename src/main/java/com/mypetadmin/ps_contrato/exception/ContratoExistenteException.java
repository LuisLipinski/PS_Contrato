package com.mypetadmin.ps_contrato.exception;

public class ContratoExistenteException extends RuntimeException {
    public ContratoExistenteException(String message) {
        super(message);
    }

    public ContratoExistenteException(String message, Throwable cause) {
        super(message, cause);
    }
}
