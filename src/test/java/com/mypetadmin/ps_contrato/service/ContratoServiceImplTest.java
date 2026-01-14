package com.mypetadmin.ps_contrato.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mypetadmin.ps_contrato.client.EmpresaClient;
import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.enums.StatusContratoId;
import com.mypetadmin.ps_contrato.exception.ContratoNotFoundException;
import com.mypetadmin.ps_contrato.exception.EmpresaNaoEncontradaException;
import com.mypetadmin.ps_contrato.exception.StatusContratoNotFoundException;
import com.mypetadmin.ps_contrato.mapper.ContratoMapper;
import com.mypetadmin.ps_contrato.model.Contrato;
import com.mypetadmin.ps_contrato.model.StatusContrato;
import com.mypetadmin.ps_contrato.repository.ContratoRepository;
import com.mypetadmin.ps_contrato.repository.StatusContratoRepository;
import com.mypetadmin.ps_contrato.service.impl.ContratoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContratoServiceImplTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private StatusContratoRepository statusContratoRepository;

    @Mock
    private EmpresaClient empresaClient;

    @Mock
    private ContratoMapper mapper;

    @InjectMocks
    private ContratoServiceImpl contratoService;

    @Captor
    private ArgumentCaptor<Contrato> contratoCaptor;

    private UUID empresaId;
    private ContratoRequestDTO requestDTO;
    private StatusContrato statusContrato;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        requestDTO = new ContratoRequestDTO(empresaId);
        statusContrato = StatusContrato.builder().id(StatusContratoId.AGUARDANDO_PAGAMENTO).statusName("Aguardando pagamento").build();
    }

    @Test
    void criarContrato_sucesso() {
        when(empresaClient.existsById(empresaId)).thenReturn(mock(JsonNode.class));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.empty());
        when(contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(anyString()))
                .thenReturn(null);
        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO))
                .thenReturn(Optional.of(statusContrato));

        Contrato contratoSalvo = Contrato.builder()
                .empresaId(empresaId)
                .status(statusContrato)
                .contractNumber("202509000001")
                .dataCriacao(LocalDateTime.now())
                .build();

        when(contratoRepository.save(any(Contrato.class))).thenReturn(contratoSalvo);
        when(mapper.toResponseDto(any(Contrato.class))).thenReturn(
                ContratoResponseDTO.builder().empresaId(empresaId).numeroContrato("202509000001").build()
        );

        ContratoResponseDTO response = contratoService.criarContrato(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getEmpresaId()).isEqualTo(empresaId);
        assertThat(response.getNumeroContrato()).isEqualTo("202509000001");

        verify(contratoRepository).save(contratoCaptor.capture());
        assertThat(contratoCaptor.getValue().getEmpresaId()).isEqualTo(empresaId);
    }

    @Test
    void criarContrato_empresaNaoEncontrada() {
        when(empresaClient.existsById(empresaId)).thenThrow(mock(feign.FeignException.NotFound.class));

        assertThrows(EmpresaNaoEncontradaException.class, () -> contratoService.criarContrato(requestDTO));
    }

    @Test
    void criarContrato_contratoExistenteAtivo() {
        when(empresaClient.existsById(empresaId)).thenReturn(mock(JsonNode.class));

        Contrato contratoExistente = Contrato.builder()
                .empresaId(empresaId)
                .status(StatusContrato.builder().id(StatusContratoId.ATIVO).statusName("Ativo").build())
                .build();

        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.of(contratoExistente));

        assertThrows(IllegalStateException.class, () -> contratoService.criarContrato(requestDTO));
    }

    @Test
    void criarContrato_statusInicialNaoEncontrado() {
        when(empresaClient.existsById(empresaId)).thenReturn(mock(JsonNode.class));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.empty());
        when(contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(anyString()))
                .thenReturn(null);

        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> contratoService.criarContrato(requestDTO));
    }

    @Test
    void atualizarStatus_quandoContratoNaoExiste_develancarExcecao() {
        UUID contratoId = UUID.randomUUID();

        when(contratoRepository.findById(contratoId))
                .thenReturn(Optional.empty());

        assertThrows(ContratoNotFoundException.class,
                () -> contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO));

        verify(contratoRepository, never()).save(any());
    }

    @Test
    void atualizarStatus_quandoStatusNaoExiste_deveLancarExcecao() {
        UUID contratoId = UUID.randomUUID();

        Contrato contrato = Contrato.builder()
                .id(contratoId)
                .empresaId(empresaId)
                .status(statusContrato)
                .build();

        when(contratoRepository.findById(contratoId))
                .thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(99L))
                .thenReturn(Optional.empty());
        assertThrows(StatusContratoNotFoundException.class,
                () -> contratoService.atualizarStatus(contratoId, 99L));
        verify(contratoRepository, never()).save(any());
    }

    @Test
    void atualizarStatus_quandoDadosValidos_deveAtualizarStatus() {
        UUID contratoId = UUID.randomUUID();

        StatusContrato novoStatus = StatusContrato.builder()
                .id(StatusContratoId.ATIVO)
                .statusName("Ativo")
                .build();

        Contrato contrato = Contrato.builder()
                .id(contratoId)
                .empresaId(empresaId)
                .status(statusContrato)
                .build();
        when(contratoRepository.findById(contratoId))
                .thenReturn(Optional.of(contrato));

        when(statusContratoRepository.findById(StatusContratoId.ATIVO))
                .thenReturn(Optional.of(novoStatus));

        when(contratoRepository.save(any(Contrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(mapper.toResponseDto(any(Contrato.class)))
                .thenReturn(ContratoResponseDTO.builder()
                        .id(contratoId)
                        .statusName("Ativo")
                        .build());

        ContratoResponseDTO response =
                contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO);

        assertThat(response).isNotNull();
        assertThat(response.getStatusName()).isEqualTo("Ativo");

        verify(contratoRepository).save(contratoCaptor.capture());
        assertThat(contratoCaptor.getValue().getStatus().getId())
                .isEqualTo(StatusContratoId.ATIVO);
    }

    @Test
    void atualizarStatus_deveAtualizarDataAtualizacaoStatus() {
        UUID contratoId = UUID.randomUUID();

        StatusContrato novoStatus = StatusContrato.builder()
                .id(StatusContratoId.INATIVO)
                .statusName("Inativo")
                .build();

        Contrato contrato = Contrato.builder()
                .id(contratoId)
                .empresaId(empresaId)
                .status(statusContrato)
                .build();

        when(contratoRepository.findById(contratoId))
                .thenReturn(Optional.of(contrato));

        when(statusContratoRepository.findById(StatusContratoId.INATIVO))
                .thenReturn(Optional.of(novoStatus));
        when(contratoRepository.save(any(Contrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(mapper.toResponseDto(any(Contrato.class)))
                .thenReturn(new ContratoResponseDTO());

        contratoService.atualizarStatus(contratoId, StatusContratoId.INATIVO);

        verify(contratoRepository).save(contratoCaptor.capture());

        assertThat(contratoCaptor.getValue().getDataAtualizacaoStatus())
                .isNotNull();
    }
}
