package com.mypetadmin.ps_contrato.service.impl;

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
import com.mypetadmin.ps_contrato.repository.ContratoSpecification;
import com.mypetadmin.ps_contrato.repository.StatusContratoRepository;
import com.mypetadmin.ps_contrato.service.ContratoNumeroGenerator;
import com.mypetadmin.ps_contrato.service.ContratoService;
import com.mypetadmin.ps_contrato.service.OnboardingLockService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContratoServiceImpl implements ContratoService {

    private final ContratoRepository contratoRepository;
    private final StatusContratoRepository statusContratoRepository;
    private final EmpresaClient empresaClient;
    private final ContratoMapper mapper;
    private final ContratoNumeroGenerator numeroGenerator;
    private final OnboardingLockService onboardingLockService;

    @Override
    @Transactional
    public ContratoResponseDTO criarContrato(ContratoRequestDTO dto) {
        log.debug("contract.create requested empresaId={} onboardingId={}", dto.getEmpresaId(), dto.getOnboardingId());

        onboardingLockService.lock(dto.getOnboardingId());

        Contrato contratoIdempotente = contratoRepository.findByOnboardingId(dto.getOnboardingId()).orElse(null);
        if (contratoIdempotente != null) {
            if (!contratoIdempotente.getEmpresaId().equals(dto.getEmpresaId())) {
                log.warn(
                        "contract.create onboarding-conflict onboardingId={} existingEmpresaId={} requestedEmpresaId={}",
                        dto.getOnboardingId(), contratoIdempotente.getEmpresaId(), dto.getEmpresaId()
                );
                throw new ContratoExistenteException("Onboarding já associado a outra empresa.");
            }

            log.debug(
                    "contract.create idempotent-replay onboardingId={} contractId={}",
                    dto.getOnboardingId(), contratoIdempotente.getId()
            );
            return mapper.toResponseDto(contratoIdempotente);
        }

        validarEmpresa(dto.getEmpresaId());
        validarContratoAbertoExistente(dto.getEmpresaId());

        String numeroContrato = numeroGenerator.gerarProximoNumero();
        StatusContrato statusInicial = statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)
                .orElseThrow(() -> new StatusContratoNotFoundException("Status inicial AGUARDANDO_PAGAMENTO não encontrado"));

        Contrato contrato = Contrato.builder()
                .empresaId(dto.getEmpresaId())
                .onboardingId(dto.getOnboardingId())
                .contractNumber(numeroContrato)
                .status(statusInicial)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacaoStatus(LocalDateTime.now())
                .build();

        try {
            contratoRepository.saveAndFlush(contrato);
        } catch (DataIntegrityViolationException ex) {
            throw new ContratoExistenteException("Conflito ao criar contrato para a empresa informada.", ex);
        }

        sincronizarEmpresa(contrato.getEmpresaId(), statusInicial.getId());
        log.info(
                "contract.create success contractId={} contractNumber={} empresaId={} onboardingId={} status=AGUARDANDO_PAGAMENTO",
                contrato.getId(), numeroContrato, contrato.getEmpresaId(), contrato.getOnboardingId()
        );

        return mapper.toResponseDto(contrato);
    }

    @Override
    @Transactional
    public ContratoResponseDTO confirmarPagamento(UUID id, PagamentoConfirmadoRequestDTO request) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new ContratoNotFoundException("Contrato com o id " + id + " não foi encontrado"));

        UUID paymentId = request.getPaymentId();
        UUID paymentRegistrado = contrato.getActivationPaymentId();

        if (paymentRegistrado != null) {
            if (paymentRegistrado.equals(paymentId)) {
                log.debug(
                        "contract.payment idempotent-replay contractId={} paymentId={} currentStatus={}",
                        contrato.getId(), paymentId, contrato.getStatus().getId()
                );
                return mapper.toResponseDto(contrato);
            }

            throw new PagamentoConfirmacaoInvalidaException(
                    "Contrato já possui uma confirmação de pagamento diferente registrada."
            );
        }

        if (!StatusContratoId.AGUARDANDO_PAGAMENTO.equals(contrato.getStatus().getId())) {
            throw new PagamentoConfirmacaoInvalidaException(
                    "Pagamento só pode ativar contrato em AGUARDANDO_PAGAMENTO."
            );
        }

        StatusContrato statusAtivo = statusContratoRepository.findById(StatusContratoId.ATIVO)
                .orElseThrow(() -> new StatusContratoNotFoundException("Status ATIVO não encontrado"));

        contrato.setActivationPaymentId(paymentId);
        contrato.setDataPagamentoConfirmado(request.getPaidAt());
        contrato.setStatus(statusAtivo);
        contrato.setDataAtualizacaoStatus(LocalDateTime.now());

        try {
            contratoRepository.saveAndFlush(contrato);
        } catch (DataIntegrityViolationException ex) {
            throw new PagamentoConfirmacaoInvalidaException(
                    "A confirmação de pagamento conflita com outro contrato.", ex
            );
        }

        sincronizarEmpresa(contrato.getEmpresaId(), StatusContratoId.ATIVO);
        log.info(
                "contract.payment confirmed contractId={} empresaId={} paymentId={} status=ATIVO",
                contrato.getId(), contrato.getEmpresaId(), paymentId
        );

        return mapper.toResponseDto(contrato);
    }

    @Override
    @Transactional
    public ContratoResponseDTO atualizarStatus(UUID id, Long statusId) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new ContratoNotFoundException("Contrato com o id " + id + " não foi encontrado"));

        StatusContrato novoStatus = statusContratoRepository.findById(statusId)
                .orElseThrow(() -> new StatusContratoNotFoundException("Status com o id " + statusId + " não foi encontrado"));

        Long statusAtual = contrato.getStatus().getId();
        if (statusAtual.equals(novoStatus.getId())) {
            log.debug("contract.status idempotent-replay contractId={} status={}", contrato.getId(), statusAtual);
            return mapper.toResponseDto(contrato);
        }

        validarTransicaoStatusAdministrativa(statusAtual, novoStatus.getId());

        contrato.setStatus(novoStatus);
        contrato.setDataAtualizacaoStatus(LocalDateTime.now());
        contratoRepository.saveAndFlush(contrato);

        sincronizarEmpresa(contrato.getEmpresaId(), novoStatus.getId());
        log.info(
                "contract.status changed contractId={} empresaId={} previousStatus={} currentStatus={}",
                contrato.getId(), contrato.getEmpresaId(), statusAtual, novoStatus.getId()
        );

        return mapper.toResponseDto(contrato);
    }

    private void validarEmpresa(UUID empresaId) {
        try {
            EmpresaStatusResponseDTO empresa = empresaClient.buscarStatusEmpresa(empresaId);
            if (empresa == null || empresa.empresaId() == null || !empresaId.equals(empresa.empresaId())) {
                throw new IntegracaoEmpresaException("Resposta inválida do PS_Empresa ao validar empresa " + empresaId, null);
            }
            log.debug("contract.company validated empresaId={} empresaStatus={}", empresaId, empresa.status());
        } catch (FeignException.NotFound ex) {
            throw new EmpresaNaoEncontradaException("Empresa com ID " + empresaId + " não encontrada");
        } catch (FeignException ex) {
            throw new IntegracaoEmpresaException("Falha ao consultar o PS_Empresa", ex);
        }
    }

    private void validarContratoAbertoExistente(UUID empresaId) {
        Contrato contratoExistente = contratoRepository
                .findTopByEmpresaIdOrderByDataCriacaoDesc(empresaId)
                .orElse(null);

        if (contratoExistente != null && !StatusContratoId.INATIVO.equals(contratoExistente.getStatus().getId())) {
            log.warn(
                    "contract.create rejected empresaId={} existingContractId={} existingStatus={}",
                    empresaId, contratoExistente.getId(), contratoExistente.getStatus().getId()
            );
            throw new ContratoExistenteException(
                    "Já existe um contrato com status " + contratoExistente.getStatus().getStatusName() + " para esta empresa"
            );
        }
    }

    private void sincronizarEmpresa(UUID empresaId, Long statusId) {
        String statusContrato = statusCallback(statusId);
        try {
            empresaClient.sincronizarStatusContrato(new EmpresaContratoStatusDTO(empresaId, statusContrato));
            log.debug("contract.company-status synchronized empresaId={} contractStatus={}", empresaId, statusContrato);
        } catch (FeignException ex) {
            throw new IntegracaoEmpresaException("Falha ao sincronizar status do contrato com o PS_Empresa", ex);
        }
    }

    private String statusCallback(Long statusId) {
        if (StatusContratoId.AGUARDANDO_PAGAMENTO.equals(statusId)) {
            return "AGUARDANDO_PAGAMENTO";
        }
        if (StatusContratoId.ATIVO.equals(statusId)) {
            return "ATIVO";
        }
        if (StatusContratoId.INATIVO.equals(statusId)) {
            return "INATIVO";
        }
        throw new StatusContratoNotFoundException("Status com o id " + statusId + " não possui mapeamento de integração");
    }

    private void validarTransicaoStatusAdministrativa(Long statusAtual, Long novoStatus) {
        if (StatusContratoId.ATIVO.equals(statusAtual)
                && StatusContratoId.INATIVO.equals(novoStatus)) {
            return;
        }

        throw new TransicaoStatusInvalidaException(
                "Transição administrativa de status inválida: " + statusAtual + " -> " + novoStatus
                        + ". A ativação depende de confirmação de pagamento."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> buscarContratos(UUID empresaId,
                                                     String numeroContrato,
                                                     String status,
                                                     LocalDate dataInicio,
                                                     LocalDate dataFim,
                                                     Pageable pageable) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("dataInicio não pode ser posterior a dataFim");
        }

        Specification<Contrato> spec = ContratoSpecification.filtrar(
                empresaId,
                numeroContrato,
                status,
                dataInicio,
                dataFim
        );

        Page<ContratoResponseDTO> result = contratoRepository.findAll(spec, pageable).map(mapper::toResponseDto);
        log.debug(
                "contract.search success page={} size={} total={} filteredByEmpresa={} filteredByNumber={} filteredByStatus={} filteredByDateRange={}",
                result.getNumber(), result.getSize(), result.getTotalElements(), empresaId != null,
                numeroContrato != null, status != null, dataInicio != null || dataFim != null
        );
        return result;
    }
}
