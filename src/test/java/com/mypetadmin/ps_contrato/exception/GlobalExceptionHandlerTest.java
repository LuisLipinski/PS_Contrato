package com.mypetadmin.ps_contrato.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleEmpresaNaoEncontrada_retornaNotFound() {
        EmpresaNaoEncontradaException ex = new EmpresaNaoEncontradaException("Empresa não encontrada");
        ResponseEntity<Map<String, String>> response = handler.handleEmpresaNaoEncontrada(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Empresa não encontrada", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_retornaBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");
        ResponseEntity<String> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Argumento inválido", response.getBody());
    }

    @Test
    void handleGeneric_retornaInternalServerError() {
        Exception ex = new Exception("Erro inesperado");
        ResponseEntity<String> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno no servidor. Tente novamente mais tarde.", response.getBody());
    }

    @Test
    void handleValidationErrors_retornaListaDeErros() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "contratoRequest");
        bindingResult.addError(new FieldError("contratoRequest", "empresaId", "não pode ser nulo"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().containsKey("empresaId"));
        assertEquals("não pode ser nulo", response.getBody().get("empresaId"));
    }

    @Test
    void handleMissingParams_retornaBadRequest() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("empresaId", "String");

        ResponseEntity<Map<String, String>> response = handler.handleMissingParams(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("empresaId"));
    }

    @Test
    void handleHttpMessageNotReadable_retornaBadRequest() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON mal formatado");

        ResponseEntity<Map<String, String>> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
        assertEquals("Corpo da requisição ausente ou inválido.", response.getBody().get("error"));
    }
}
