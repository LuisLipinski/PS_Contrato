package com.mypetadmin.ps_contrato.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/contratos");
    }

    @Test
    void empresaNaoEncontradaRetornaNotFoundPadronizado() {
        ResponseEntity<ErrorResponse> response = handler.handleEmpresaNaoEncontrada(
                new EmpresaNaoEncontradaException("Empresa não encontrada"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("EMPRESA_NOT_FOUND");
    }

    @Test
    void contratoExistenteRetornaConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleContratoExistente(
                new ContratoExistenteException("Contrato existente"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("CONTRATO_ALREADY_EXISTS");
    }

    @Test
    void transicaoInvalidaRetornaConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleTransicaoInvalida(
                new TransicaoStatusInvalidaException("Transição inválida"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void falhaDeIntegracaoRetornaBadGatewaySemExporCausa() {
        ResponseEntity<ErrorResponse> response = handler.handleIntegracaoEmpresa(
                new IntegracaoEmpresaException("detalhe interno", new RuntimeException("segredo técnico")), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().getCode()).isEqualTo("PS_EMPRESA_INTEGRATION_ERROR");
        assertThat(response.getBody().getMessage()).doesNotContain("segredo técnico");
    }

    @Test
    void illegalArgumentRetornaBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Argumento inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_ARGUMENT");
    }

    @Test
    void validationErrorsRetornaMapaDeCampos() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "contratoRequest");
        bindingResult.addError(new FieldError("contratoRequest", "empresaId", "não pode ser nulo"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getErrors()).containsEntry("empresaId", "não pode ser nulo");
    }

    @Test
    void missingParameterRetornaBadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("empresaId", "String");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParams(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("MISSING_PARAMETER");
    }

    @Test
    void unreadableBodyRetornaBadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON mal formatado", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_REQUEST_BODY");
    }

    @Test
    void genericErrorNaoExpoeMensagemInterna() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new Exception("detalhe interno"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Erro interno no servidor. Tente novamente mais tarde.");
    }
}
