package com.mypetadmin.ps_contrato.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmpresaNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleEmpresaNaoEncontrada(EmpresaNaoEncontradaException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "EMPRESA_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ContratoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContratoNotFound(ContratoNotFoundException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CONTRATO_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(StatusContratoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStatusNotFound(StatusContratoNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "STATUS_CONTRATO_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ContratoExistenteException.class)
    public ResponseEntity<ErrorResponse> handleContratoExistente(ContratoExistenteException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONTRATO_ALREADY_EXISTS", ex.getMessage(), request);
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransicaoInvalida(TransicaoStatusInvalidaException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage(), request);
    }

    @ExceptionHandler(PagamentoConfirmacaoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handlePagamentoConfirmacaoInvalida(PagamentoConfirmacaoInvalidaException ex,
                                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "PAYMENT_CONFIRMATION_CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(IntegracaoEmpresaException.class)
    public ResponseEntity<ErrorResponse> handleIntegracaoEmpresa(IntegracaoEmpresaException ex,
                                                                  HttpServletRequest request) {
        log.error("integration.ps-empresa failed method={} path={}", request.getMethod(), request.getRequestURI(), ex);
        return build(
                HttpStatus.BAD_GATEWAY,
                "PS_EMPRESA_INTEGRATION_ERROR",
                "Não foi possível concluir a operação por falha de integração com PS_Empresa.",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        log.warn("validation.failed method={} path={} fields={}", request.getMethod(), request.getRequestURI(), errors.keySet());
        return ResponseEntity.badRequest().body(
                ErrorResponse.validation(
                        "Um ou mais campos são inválidos.",
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        errors
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Parâmetro de requisição inválido.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Valor inválido para o parâmetro " + ex.getName() + ".",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex,
                                                              HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Parâmetro ausente: " + ex.getParameterName(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "Corpo da requisição ausente ou inválido.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
                                                        HttpServletRequest request) {
        log.error("request.unexpected-error method={} path={}", request.getMethod(), request.getRequestURI(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Erro interno no servidor. Tente novamente mais tarde.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String code,
                                                String message,
                                                HttpServletRequest request) {
        if (status.is4xxClientError()) {
            log.warn("request.rejected code={} method={} path={} message={}", code, request.getMethod(), request.getRequestURI(), message);
        }
        return ResponseEntity.status(status).body(
                ErrorResponse.of(code, message, status.value(), request.getRequestURI())
        );
    }
}
