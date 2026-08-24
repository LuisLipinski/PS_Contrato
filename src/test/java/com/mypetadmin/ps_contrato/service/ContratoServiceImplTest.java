package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.client.EmpresaClient;
import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.EmpresaContratoStatusDTO;
import com.mypetadmin.ps_contrato.dto.EmpresaStatusResponseDTO;
import com.mypetadmin.ps_contrato.enums.StatusContratoId;
import com.mypetadmin.ps_contrato.exception.ContratoExistenteException;
import com.mypetadmin.ps_contrato.exception.ContratoNotFoundException;
import com.mypetadmin.ps_contrato.exception.EmpresaNaoEncontradaException;
import com.mypetadmin.ps_contrato.exception.IntegracaoEmpresaException;
import com.mypetadmin.ps_contrato.exception.StatusContratoNotFoundException;
import com.mypetadmin.ps_contrato.exception.TransicaoStatusInvalidaException;
import com.mypetadmin.ps_contrato.mapper.ContratoMapper;
import com.mypetadmin.ps_contrato.model.Contrato;
import com.mypetadmin.ps_contrato.model.StatusContrato;
import com.mypetadmin.ps_contrato.repository.ContratoRepository;
import com.mypetadmin.ps_contrato.repository.StatusContratoRepository;
import com.mypetadmin.ps_contrato.service.impl.ContratoServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceImplTest {

    @Mock
    private ContratoRepository contratoRepository;
    @Mock
    private StatusContratoRepository statusContratoRepository;
    @Mock
    private EmpresaClient empresaClient;
    @Mock
    private ContratoMapper mapper;

    private ContratoServiceImpl contratoService;
    private UUID empresaId;
    private StatusContrato aguardando;
    private StatusContrato ativo;
    private StatusContrato inativo;

    @BeforeEach
    void setUp() {
        contratoService = new ContratoServiceImpl(contratoRepository, statusContratoRepository, empresaClient, mapper);
        empresaId = UUID.randomUUID();
        aguardando = status(StatusContratoId.AGUARDANDO_PAGAMENTO, "Aguardando pagamento");
        ativo = status(StatusContratoId.ATIVO, "Ativo");
        inativo = status(StatusContratoId.INATIVO, "Inativo");
    }

    @Test
    void criarContratoDeveValidarEmpresaPersistirESincronizarStatus() {
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "AGUARDANDO_CONTRATO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId)).thenReturn(Optional.empty());
        when(contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(anyString())).thenReturn(null);
        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)).thenReturn(Optional.of(aguardando));

        ContratoResponseDTO response = ContratoResponseDTO.builder().empresaId(empresaId).build();
        when(mapper.toResponseDto(any(Contrato.class))).thenReturn(response);

        ContratoResponseDTO result = contratoService.criarContrato(new ContratoRequestDTO(empresaId));

        assertThat(result).isSameAs(response);

        ArgumentCaptor<Contrato> contratoCaptor = ArgumentCaptor.forClass(Contrato.class);
        verify(contratoRepository).saveAndFlush(contratoCaptor.capture());
        assertThat(contratoCaptor.getValue().getContractNumber()).endsWith("000001");
        assertThat(contratoCaptor.getValue().getStatus().getId()).isEqualTo(StatusContratoId.AGUARDANDO_PAGAMENTO);
        assertThat(contratoCaptor.getValue().getDataAtualizacaoStatus()).isNotNull();

        ArgumentCaptor<EmpresaContratoStatusDTO> callbackCaptor = ArgumentCaptor.forClass(EmpresaContratoStatusDTO.class);
        verify(empresaClient).sincronizarStatusContrato(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().empresaId()).isEqualTo(empresaId);
        assertThat(callbackCaptor.getValue().statusContrato()).isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    void criarContratoDeveFalharQuandoEmpresaNaoExiste() {
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId)))
                .isInstanceOf(EmpresaNaoEncontradaException.class);

        verify(contratoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarContratoNaoDeveProsseguirQuandoPsEmpresaFalha() {
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId)))
                .isInstanceOf(IntegracaoEmpresaException.class);

        verify(contratoRepository, never()).findTopByEmpresaIdOrderByDataCriacaoDesc(any());
        verify(contratoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarContratoDeveRejeitarContratoNaoInativoExistente() {
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "ATIVO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.of(Contrato.builder().empresaId(empresaId).status(ativo).build()));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId)))
                .isInstanceOf(ContratoExistenteException.class);
    }

    @Test
    void criarContratoDevePermitirNovoContratoAposUltimoInativo() {
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "INATIVO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.of(Contrato.builder().empresaId(empresaId).status(inativo).build()));
        when(contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(anyString())).thenReturn(null);
        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)).thenReturn(Optional.of(aguardando));
        when(mapper.toResponseDto(any())).thenReturn(new ContratoResponseDTO());

        contratoService.criarContrato(new ContratoRequestDTO(empresaId));

        verify(contratoRepository).saveAndFlush(any(Contrato.class));
    }

    @Test
    void criarContratoDeveFalharQuandoStatusInicialNaoExiste() {
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "AGUARDANDO_CONTRATO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId)).thenReturn(Optional.empty());
        when(contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(anyString())).thenReturn(null);
        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId)))
                .isInstanceOf(StatusContratoNotFoundException.class);
    }

    @Test
    void atualizarStatusAguardandoParaAtivoDevePersistirESincronizarEmpresa() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));
        when(mapper.toResponseDto(contrato)).thenReturn(ContratoResponseDTO.builder().statusName("Ativo").build());

        ContratoResponseDTO result = contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO);

        assertThat(result.getStatusName()).isEqualTo("Ativo");
        assertThat(contrato.getStatus()).isSameAs(ativo);
        assertThat(contrato.getDataAtualizacaoStatus()).isNotNull();
        verify(contratoRepository).saveAndFlush(contrato);
        verify(empresaClient).sincronizarStatusContrato(new EmpresaContratoStatusDTO(empresaId, "ATIVO"));
    }

    @Test
    void atualizarStatusAtivoParaInativoDeveSincronizarEmpresa() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, ativo);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.INATIVO)).thenReturn(Optional.of(inativo));
        when(mapper.toResponseDto(contrato)).thenReturn(new ContratoResponseDTO());

        contratoService.atualizarStatus(contratoId, StatusContratoId.INATIVO);

        verify(empresaClient).sincronizarStatusContrato(new EmpresaContratoStatusDTO(empresaId, "INATIVO"));
    }

    @Test
    void atualizarMesmoStatusDeveSerIdempotente() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, ativo);
        ContratoResponseDTO response = ContratoResponseDTO.builder().statusName("Ativo").build();
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));
        when(mapper.toResponseDto(contrato)).thenReturn(response);

        assertThat(contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO)).isSameAs(response);

        verify(contratoRepository, never()).saveAndFlush(any());
        verify(empresaClient, never()).sincronizarStatusContrato(any());
    }

    @Test
    void atualizarStatusDeveRejeitarTransicaoInvalida() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, inativo);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));

        assertThatThrownBy(() -> contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void atualizarStatusDeveFalharQuandoContratoNaoExiste() {
        UUID contratoId = UUID.randomUUID();
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO))
                .isInstanceOf(ContratoNotFoundException.class);
    }

    @Test
    void atualizarStatusDeveFalharQuandoStatusNaoExiste() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.atualizarStatus(contratoId, 99L))
                .isInstanceOf(StatusContratoNotFoundException.class);
    }

    @Test
    void atualizarStatusDeveFalharQuandoCallbackDaEmpresaFalha() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));
        doThrow(mock(FeignException.class))
                .when(empresaClient).sincronizarStatusContrato(any(EmpresaContratoStatusDTO.class));

        assertThatThrownBy(() -> contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO))
                .isInstanceOf(IntegracaoEmpresaException.class);
    }

    @Test
    void buscarContratosDeveRejeitarIntervaloDeDatasInvertido() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> contratoService.buscarContratos(
                null, null, null,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1),
                pageable
        )).isInstanceOf(IllegalArgumentException.class);

        verify(contratoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void buscarContratosComFiltrosDeveExecutarUmaConsulta() {
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "dataCriacao");
        when(contratoRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

        Page<ContratoResponseDTO> result = contratoService.buscarContratos(
                empresaId,
                "202608",
                "Ativo",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                pageable
        );

        assertThat(result).isEmpty();
        verify(contratoRepository).findAll(any(Specification.class), eq(pageable));
    }

    private Contrato contrato(UUID id, StatusContrato status) {
        return Contrato.builder()
                .id(id)
                .empresaId(empresaId)
                .contractNumber("202608000001")
                .status(status)
                .build();
    }

    private StatusContrato status(Long id, String nome) {
        return StatusContrato.builder()
                .id(id)
                .statusName(nome)
                .descricao(nome)
                .build();
    }
}
