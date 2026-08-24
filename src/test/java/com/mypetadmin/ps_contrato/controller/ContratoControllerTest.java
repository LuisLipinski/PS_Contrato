package com.mypetadmin.ps_contrato.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.ContratoStatusUpdateDTO;
import com.mypetadmin.ps_contrato.exception.ContratoNotFoundException;
import com.mypetadmin.ps_contrato.exception.EmpresaNaoEncontradaException;
import com.mypetadmin.ps_contrato.exception.StatusContratoNotFoundException;
import com.mypetadmin.ps_contrato.security.InternalRequestFilter;
import com.mypetadmin.ps_contrato.security.SecurityConfig;
import com.mypetadmin.ps_contrato.service.ContratoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContratoController.class)
@Import({SecurityConfig.class, InternalRequestFilter.class})
@TestPropertySource(properties = "security.internal-key=test-internal-key")
class ContratoControllerTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContratoService contratoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void endpointsDeContratoDevemExigirChaveInterna() throws Exception {
        mockMvc.perform(get("/contratos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chaveInternaInvalidaDeveSerRejeitada() throws Exception {
        mockMvc.perform(get("/contratos").header("X-Internal-Key", "invalida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarContratoQuandoDadosValidosRetornaCreated() throws Exception {
        UUID empresaId = UUID.randomUUID();
        UUID contratoId = UUID.randomUUID();
        ContratoRequestDTO requestDTO = new ContratoRequestDTO(empresaId);

        ContratoResponseDTO responseDTO = ContratoResponseDTO.builder()
                .id(contratoId)
                .empresaId(empresaId)
                .numeroContrato("202608000001")
                .statusName("Aguardando pagamento")
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacaoStatus(LocalDateTime.now())
                .build();

        when(contratoService.criarContrato(any(ContratoRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/contratos")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/contratos/" + contratoId)))
                .andExpect(jsonPath("$.empresaId").value(empresaId.toString()))
                .andExpect(jsonPath("$.numeroContrato").value("202608000001"))
                .andExpect(jsonPath("$.statusName").value("Aguardando pagamento"));
    }

    @Test
    void criarContratoQuandoEmpresaIdNuloRetornaBadRequest() throws Exception {
        ContratoRequestDTO requestDTO = ContratoRequestDTO.builder().empresaId(null).build();

        mockMvc.perform(post("/contratos")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void criarContratoQuandoEmpresaNaoExisteRetornaNotFoundPadronizado() throws Exception {
        UUID empresaId = UUID.randomUUID();
        when(contratoService.criarContrato(any(ContratoRequestDTO.class)))
                .thenThrow(new EmpresaNaoEncontradaException("Empresa não encontrada"));

        mockMvc.perform(post("/contratos")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContratoRequestDTO(empresaId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPRESA_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Empresa não encontrada"));
    }

    @Test
    void atualizarStatusQuandoStatusIdNuloRetornaBadRequest() throws Exception {
        UUID contratoId = UUID.randomUUID();
        ContratoStatusUpdateDTO requestDTO = new ContratoStatusUpdateDTO();

        mockMvc.perform(patch("/contratos/{id}/status", contratoId)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void atualizarStatusQuandoContratoNaoExisteRetornaNotFound() throws Exception {
        UUID contratoId = UUID.randomUUID();
        ContratoStatusUpdateDTO requestDTO = new ContratoStatusUpdateDTO();
        requestDTO.setStatusId(2L);

        when(contratoService.atualizarStatus(eq(contratoId), eq(2L)))
                .thenThrow(new ContratoNotFoundException("Contrato não encontrado"));

        mockMvc.perform(patch("/contratos/{id}/status", contratoId)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRATO_NOT_FOUND"));
    }

    @Test
    void atualizarStatusQuandoStatusNaoExisteRetornaNotFound() throws Exception {
        UUID contratoId = UUID.randomUUID();
        ContratoStatusUpdateDTO requestDTO = new ContratoStatusUpdateDTO();
        requestDTO.setStatusId(99L);

        when(contratoService.atualizarStatus(eq(contratoId), eq(99L)))
                .thenThrow(new StatusContratoNotFoundException("Status não encontrado"));

        mockMvc.perform(patch("/contratos/{id}/status", contratoId)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STATUS_CONTRATO_NOT_FOUND"));
    }

    @Test
    void buscarContratosComFiltroDeDataRetornaOkEConsultaUmaVez() throws Exception {
        when(contratoService.buscarContratos(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/contratos")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "DATA_CRIACAO")
                        .param("direction", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(contratoService, times(1)).buscarContratos(any(), any(), any(), any(), any(), any());
    }

    @Test
    void buscarContratosDeveLimitarTamanhoDaPagina() throws Exception {
        mockMvc.perform(get("/contratos")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }
}
