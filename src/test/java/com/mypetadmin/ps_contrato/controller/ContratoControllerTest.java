package com.mypetadmin.ps_contrato.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.ContratoStatusUpdateDTO;
import com.mypetadmin.ps_contrato.exception.ContratoNotFoundException;
import com.mypetadmin.ps_contrato.exception.EmpresaNaoEncontradaException;
import com.mypetadmin.ps_contrato.exception.StatusContratoNotFoundException;
import com.mypetadmin.ps_contrato.service.ContratoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContratoController.class)
public class ContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContratoService contratoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void criarContrato_quandoDadosValidos_retornaCreated() throws Exception {
        UUID empresaId = UUID.randomUUID();

        ContratoRequestDTO requestDTO = new ContratoRequestDTO();
        requestDTO.setEmpresaId(empresaId);

        ContratoResponseDTO responseDTO = ContratoResponseDTO.builder()
                .id(UUID.randomUUID())
                .empresaId(empresaId)
                .numeroContrato("CT-12345")
                .statusName("Aguardando Pagamento")
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacaoStatus(LocalDateTime.now())
                .build();

        when(contratoService.criarContrato(any(ContratoRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/contratos/criarContrato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresaId").value(empresaId.toString()))
                .andExpect(jsonPath("$.numeroContrato").value("CT-12345"))
                .andExpect(jsonPath("$.statusName").value("Aguardando Pagamento"))
                .andExpect(jsonPath("$.dataCriacao").exists())
                .andExpect(jsonPath("$.dataAtualizacaoStatus").exists());
    }

    @Test
    void criarContrato_quandoEmpresaIdNulo_retornaBadRequest() throws Exception {
        ContratoRequestDTO requestDTO = ContratoRequestDTO.builder()
                .empresaId(null)
                .build();

        mockMvc.perform(post("/contratos/criarContrato")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarContrato_quandoEmpresaNaoExiste_retornandoNotFound() throws Exception {
        UUID empresaIdInexistente = UUID.randomUUID();
        ContratoRequestDTO requestDTO = new ContratoRequestDTO();
        requestDTO.setEmpresaId(empresaIdInexistente);
        when(contratoService.criarContrato(any(ContratoRequestDTO.class)))
                .thenThrow(new EmpresaNaoEncontradaException("Empresa não encontrada"));

        mockMvc.perform(post("/contratos/criarContrato")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Empresa não encontrada"));
    }

    @Test
    void atualizarStatus_quandoStatusIdNulo_retornaBadRequest() throws Exception {
        UUID contratoId = UUID.randomUUID();

        ContratoStatusUpdateDTO requestDTO = new ContratoStatusUpdateDTO();
        requestDTO.setStatusId(null);

        mockMvc.perform(put("/contratos/{id}/status", contratoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarStatus_quandoContratoNaoExiste_retornaNotFound() throws Exception {
        UUID contratoid = UUID.randomUUID();

        ContratoStatusUpdateDTO requestDTO = new ContratoStatusUpdateDTO();
        requestDTO.setStatusId(2L);

        when(contratoService.atualizarStatus(eq(contratoid), eq(2L)))
                .thenThrow(new ContratoNotFoundException("Contrato não encontrado"));

        mockMvc.perform(put("/contratos/{id}/status", contratoid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Contrato não encontrado"));
    }

    @Test
    void atualizarStatus_quandoStatusNaoExiste_retornaNotFound() throws Exception {
        UUID contratoId = UUID.randomUUID();

        ContratoStatusUpdateDTO requestDTO = new ContratoStatusUpdateDTO();
        requestDTO.setStatusId(99L);

        when(contratoService.atualizarStatus(eq(contratoId), eq(99L)))
                .thenThrow(new StatusContratoNotFoundException("Status não encontrado"));

        mockMvc.perform(put("/contratos/{id}/status", contratoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Status não encontrado"));
    }

}
