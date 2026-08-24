package com.mypetadmin.ps_contrato.service.impl;

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
import com.mypetadmin.ps_contrato.repository.ContratoSpecification;
import com.mypetadmin.ps_contrato.repository.StatusContratoRepository;
import com.mypetadmin.ps_contrato.service.ContratoService;
import com.mypetadmin.ps_contrato.util.GerarNumeroContratoUtil;
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

    @Override
    @Transactional
    public ContratoResponseDTO criarContrato(ContratoRequestDTO dto) {
        validarEmpresa(dto.getEmpresaId());
        validarContratoAbertoExistente(dto.getEmpresaId());

        String prefixo = String.format("%04d%02d", LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        Contrato ultimoContrato = contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(prefixo);
        long sequencial = ultimoContrato == null
                ? 1L
                : Long.parseLong(ultimoContrato.getContractNumber().substring(6)) + 1L;

        String numeroContrato = GerarNumeroContratoUtil.gerarNumeroContrato(sequencial);
        StatusContrato statusInicial = statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)
                .orElseThrow(() -> new StatusContratoNotFoundException("Status inicial AGUARDANDO_PAGAMENTO não encontrado"));

        Contrato contrato = Contrato.builder()
                .empresaId(dto.getEmpresaId())
                .contractNumber(numeroContrato)
                .status(statusInicial)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacaoStatus(LocalDateTime.now())
                .build();

        try {
            contratoRepository.saveAndFlush(contrato);
        } catch (DataIntegrityViolationException ex) {
            throw new ContratoExistenteException("Conflito ao criar contrato para a empresa " + dto.getEmpresaId(), ex);
        }

        sincronizarEmpresa(contrato.getEmpresaId(), statusInicial.getId());
        log.info("Contrato {} criado para empresa {} com status AGUARDANDO_PAGAMENTO", numeroContrato, contrato.getEmpresaId());

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
            return mapper.toResponseDto(contrato);
        }

        validarTransicaoStatus(statusAtual, novoStatus.getId());

        contrato.setStatus(novoStatus);
        contrato.setDataAtualizacaoStatus(LocalDateTime.now());
        contratoRepository.saveAndFlush(contrato);

        sincronizarEmpresa(contrato.getEmpresaId(), novoStatus.getId());
        log.info("Contrato {} alterado do status {} para {}", contrato.getContractNumber(), statusAtual, novoStatus.getId());

        return mapper.toResponseDto(contrato);
    }

    private void validarEmpresa(UUID empresaId) {
        try {
            EmpresaStatusResponseDTO empresa = empresaClient.buscarStatusEmpresa(empresaId);
            if (empresa == null || empresa.id() == null || !empresaId.equals(empresa.id())) {
                throw new IntegracaoEmpresaException("Resposta inválida do PS_Empresa ao validar empresa " + empresaId, null);
            }
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
            throw new ContratoExistenteException(
                    "Já existe um contrato com status " + contratoExistente.getStatus().getStatusName() + " para esta empresa"
            );
        }
    }

    private void sincronizarEmpresa(UUID empresaId, Long statusId) {
        String statusContrato = statusCallback(statusId);
        try {
            empresaClient.sincronizarStatusContrato(new EmpresaContratoStatusDTO(empresaId, statusContrato));
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

    private void validarTransicaoStatus(Long statusAtual, Long novoStatus) {
        if (StatusContratoId.AGUARDANDO_PAGAMENTO.equals(statusAtual)
                && StatusContratoId.ATIVO.equals(novoStatus)) {
            return;
        }

        if (StatusContratoId.ATIVO.equals(statusAtual)
                && StatusContratoId.INATIVO.equals(novoStatus)) {
            return;
        }

        throw new TransicaoStatusInvalidaException("Transição de status inválida: " + statusAtual + " -> " + novoStatus);
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

        return contratoRepository.findAll(spec, pageable).map(mapper::toResponseDto);
    }
}
