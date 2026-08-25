package com.mypetadmin.ps_contrato.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final int status;
    private final LocalDateTime timestamp;
    private final String path;
    private final Map<String, String> errors;

    public ErrorResponse(String code,
                         String message,
                         int status,
                         String path,
                         Map<String, String> errors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.errors = errors == null ? Collections.emptyMap() : errors;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(String code, String message, int status, String path) {
        return new ErrorResponse(code, message, status, path, Collections.emptyMap());
    }

    public static ErrorResponse validation(String message,
                                           int status,
                                           String path,
                                           Map<String, String> errors) {
        return new ErrorResponse("VALIDATION_ERROR", message, status, path, errors);
    }
}
