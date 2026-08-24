package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.client.EmpresaClient;
import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.EmpresaContratoStatusDTO;
import com.mypetadmin.ps_contrato.dto.EmpresaStatusResponseDTO;
import com.mypetadmin.ps_contrato.dto.PagamentoConfirmadoRequestDTO;
import com.mypetadmin.ps_contrato.enums.StatusContratoId;
import com.mypetadmin.ps_contrato.exception.ContratoExistenteException;
import com.mypetadmin.ps_contrato.exception.ContratoNotFoundException;
import com.mypetadmin.ps_contrato.exception.EmpresaNaoEncontradaException;
import com.mypetadmin.ps_contrato.exception.IntegracaoEmpresaException;
import com.mypetadmin.ps_contrato.exception.PagamentoConfirmacaoInvalidaException;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private ContratoNumeroGenerator numeroGenerator;
    @Mock
    private OnboardingLockService onboardingLockService;

    private ContratoServiceImpl contratoService;
    private UUID empresaId;
    private UUID onboardingId;
    private StatusContrato aguardando;
    private StatusContrato ativo;
    private StatusContrato inativo;

    @BeforeEach
    void setUp() {
        contratoService = new ContratoServiceImpl(
                contratoRepository,
                statusContratoRepository,
                empresaClient,
                mapper,
                numeroGenerator,
                onboardingLockService
        );
        empresaId = UUID.randomUUID();
        onboardingId = UUID.randomUUID();
        aguardando = status(StatusContratoId.AGUARDANDO_PAGAMENTO, "Aguardando pagamento");
        ativo = status(StatusContratoId.ATIVO, "Ativo");
        inativo = status(StatusContratoId.INATIVO, "Inativo");
    }

    @Test
    void criarContratoDeveValidarEmpresaPersistirESincronizarStatus() {
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.empty());
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "AGUARDANDO_CONTRATO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId)).thenReturn(Optional.empty());
        when(numeroGenerator.gerarProximoNumero()).thenReturn("202608000001");
        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)).thenReturn(Optional.of(aguardando));

        ContratoResponseDTO response = ContratoResponseDTO.builder().empresaId(empresaId).build();
        when(mapper.toResponseDto(any(Contrato.class))).thenReturn(response);

        ContratoResponseDTO result = contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId));

        assertThat(result).isSameAs(response);
        verify(onboardingLockService).lock(onboardingId);

        ArgumentCaptor<Contrato> contratoCaptor = ArgumentCaptor.forClass(Contrato.class);
        verify(contratoRepository).saveAndFlush(contratoCaptor.capture());
        assertThat(contratoCaptor.getValue().getContractNumber()).isEqualTo("202608000001");
        assertThat(contratoCaptor.getValue().getOnboardingId()).isEqualTo(onboardingId);
        assertThat(contratoCaptor.getValue().getStatus().getId()).isEqualTo(StatusContratoId.AGUARDANDO_PAGAMENTO);

        ArgumentCaptor<EmpresaContratoStatusDTO> callbackCaptor = ArgumentCaptor.forClass(EmpresaContratoStatusDTO.class);
        verify(empresaClient).sincronizarStatusContrato(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().empresaId()).isEqualTo(empresaId);
        assertThat(callbackCaptor.getValue().statusContrato()).isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    void criarContratoDeveSerIdempotenteParaMesmoOnboarding() {
        Contrato existente = Contrato.builder()
                .id(UUID.randomUUID())
                .empresaId(empresaId)
                .onboardingId(onboardingId)
                .contractNumber("202608000001")
                .status(aguardando)
                .build();
        ContratoResponseDTO response = ContratoResponseDTO.builder().id(existente.getId()).build();
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.of(existente));
        when(mapper.toResponseDto(existente)).thenReturn(response);

        assertThat(contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId))).isSameAs(response);

        verify(onboardingLockService).lock(onboardingId);
        verify(empresaClient, never()).buscarStatusEmpresa(any());
        verify(numeroGenerator, never()).gerarProximoNumero();
        verify(contratoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarContratoDeveRejeitarOnboardingJaAssociadoAOutraEmpresa() {
        Contrato existente = Contrato.builder()
                .id(UUID.randomUUID())
                .empresaId(UUID.randomUUID())
                .onboardingId(onboardingId)
                .status(aguardando)
                .build();
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId)))
                .isInstanceOf(ContratoExistenteException.class);
    }

    @Test
    void criarContratoDeveFalharQuandoEmpresaNaoExiste() {
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.empty());
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId)))
                .isInstanceOf(EmpresaNaoEncontradaException.class);

        verify(contratoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarContratoNaoDeveProsseguirQuandoPsEmpresaFalha() {
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.empty());
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId)))
                .isInstanceOf(IntegracaoEmpresaException.class);

        verify(contratoRepository, never()).findTopByEmpresaIdOrderByDataCriacaoDesc(any());
        verify(contratoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarContratoDeveRejeitarContratoNaoInativoExistente() {
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.empty());
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "ATIVO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.of(Contrato.builder().empresaId(empresaId).status(ativo).build()));

        assertThatThrownBy(() -> contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId)))
                .isInstanceOf(ContratoExistenteException.class);
    }

    @Test
    void criarContratoDevePermitirNovoContratoAposUltimoInativo() {
        when(contratoRepository.findByOnboardingId(onboardingId)).thenReturn(Optional.empty());
        when(empresaClient.buscarStatusEmpresa(empresaId)).thenReturn(new EmpresaStatusResponseDTO(empresaId, "INATIVO"));
        when(contratoRepository.findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId))
                .thenReturn(Optional.of(Contrato.builder().empresaId(empresaId).status(inativo).build()));
        when(numeroGenerator.gerarProximoNumero()).thenReturn("202608000002");
        when(statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)).thenReturn(Optional.of(aguardando));
        when(mapper.toResponseDto(any())).thenReturn(new ContratoResponseDTO());

        contratoService.criarContrato(new ContratoRequestDTO(empresaId, onboardingId));

        verify(contratoRepository).saveAndFlush(any(Contrato.class));
    }

    @Test
    void confirmarPagamentoDeveAtivarContratoESincronizarEmpresa() {
        UUID contratoId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDateTime paidAt = LocalDateTime.now().minusMinutes(1);
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));
        when(mapper.toResponseDto(contrato)).thenReturn(ContratoResponseDTO.builder().statusName("Ativo").build());

        ContratoResponseDTO result = contratoService.confirmarPagamento(
                contratoId,
                new PagamentoConfirmadoRequestDTO(paymentId, paidAt)
        );

        assertThat(result.getStatusName()).isEqualTo("Ativo");
        assertThat(contrato.getStatus()).isSameAs(ativo);
        assertThat(contrato.getActivationPaymentId()).isEqualTo(paymentId);
        assertThat(contrato.getDataPagamentoConfirmado()).isEqualTo(paidAt);
        verify(contratoRepository).saveAndFlush(contrato);
        verify(empresaClient).sincronizarStatusContrato(new EmpresaContratoStatusDTO(empresaId, "ATIVO"));
    }

    @Test
    void confirmarPagamentoDeveSerIdempotenteParaMesmoPaymentId() {
        UUID contratoId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, ativo);
        contrato.setActivationPaymentId(paymentId);
        ContratoResponseDTO response = ContratoResponseDTO.builder().statusName("Ativo").build();
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(mapper.toResponseDto(contrato)).thenReturn(response);

        assertThat(contratoService.confirmarPagamento(
                contratoId,
                new PagamentoConfirmadoRequestDTO(paymentId, LocalDateTime.now())
        )).isSameAs(response);

        verify(contratoRepository, never()).saveAndFlush(any());
        verify(empresaClient, never()).sincronizarStatusContrato(any());
    }

    @Test
    void confirmarPagamentoDeveRejeitarPaymentIdDiferenteAposAtivacao() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, ativo);
        contrato.setActivationPaymentId(UUID.randomUUID());
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.confirmarPagamento(
                contratoId,
                new PagamentoConfirmadoRequestDTO(UUID.randomUUID(), LocalDateTime.now())
        )).isInstanceOf(PagamentoConfirmacaoInvalidaException.class);
    }

    @Test
    void confirmarPagamentoDeveFalharQuandoContratoNaoExiste() {
        UUID contratoId = UUID.randomUUID();
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.confirmarPagamento(
                contratoId,
                new PagamentoConfirmadoRequestDTO(UUID.randomUUID(), LocalDateTime.now())
        )).isInstanceOf(ContratoNotFoundException.class);
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
    void atualizarStatusNaoDevePermitirAtivacaoAdministrativa() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));

        assertThatThrownBy(() -> contratoService.atualizarStatus(contratoId, StatusContratoId.ATIVO))
                .isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("pagamento");
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
    void atualizarStatusDeveFalharQuandoStatusNaoExiste() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.atualizarStatus(contratoId, 99L))
                .isInstanceOf(StatusContratoNotFoundException.class);
    }

    @Test
    void confirmarPagamentoDeveFalharQuandoCallbackDaEmpresaFalha() {
        UUID contratoId = UUID.randomUUID();
        Contrato contrato = contrato(contratoId, aguardando);
        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));
        when(statusContratoRepository.findById(StatusContratoId.ATIVO)).thenReturn(Optional.of(ativo));
        doThrow(mock(FeignException.class))
                .when(empresaClient).sincronizarStatusContrato(any(EmpresaContratoStatusDTO.class));

        assertThatThrownBy(() -> contratoService.confirmarPagamento(
                contratoId,
                new PagamentoConfirmadoRequestDTO(UUID.randomUUID(), LocalDateTime.now())
        )).isInstanceOf(IntegracaoEmpresaException.class);
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
                .onboardingId(onboardingId)
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
