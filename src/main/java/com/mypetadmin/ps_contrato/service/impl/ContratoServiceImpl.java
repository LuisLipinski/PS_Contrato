package com.mypetadmin.ps_contrato.service.impl;

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
import com.mypetadmin.ps_contrato.service.ContratoService;
import com.mypetadmin.ps_contrato.util.GerarNumeroContratoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.mypetadmin.ps_contrato.util.GerarNumeroContratoUtil.gerarNumeroContrato;

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
        boolean exists;
        try {
            Object response = empresaClient.existsById(dto.getEmpresaId());
            exists = response != null;
        } catch (feign.FeignException.NotFound e) {
            exists = false;
            log.error("Empresa com o id {} não foi encontrada", dto.getEmpresaId());
            throw new EmpresaNaoEncontradaException("Empresa com ID " + dto.getEmpresaId() + " não encontrada");
        } catch (Exception e) {
            exists = false; // outros erros
        }

        Contrato contratoExistente = contratoRepository
                .findTopByEmpresaIdOrderByDataCriacaoDesc(dto.getEmpresaId())
                .orElse(null);

        if (contratoExistente != null) {
            Long statusId = contratoExistente.getStatus().getId();
            if (!statusId.equals(StatusContratoId.INATIVO)) {
                throw new IllegalStateException("Já existe um contrato com status "
                        + contratoExistente.getStatus().getStatusName() +
                        " para esta empresa");
            }
        }

        LocalDate hoje = LocalDate.now();
        int ano = hoje.getYear();
        int mes = hoje.getMonthValue();

        String prefixo = String.format("%04d%02d", ano, mes);

        Contrato ultimoContrato = contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(prefixo);
        long sequencial = ultimoContrato == null ? 0 : Long.parseLong(ultimoContrato.getContractNumber().substring(6)) + 1;

        String numeroContrato = GerarNumeroContratoUtil.gerarNumeroContrato(sequencial);

        StatusContrato statusContrato = statusContratoRepository.findById(StatusContratoId.AGUARDANDO_PAGAMENTO)
                .orElseThrow(() -> new IllegalStateException("Status inicial não encontrado"));

        Contrato contrato = Contrato.builder()
                .empresaId(dto.getEmpresaId())
                .contractNumber(numeroContrato)
                .status(statusContrato)
                .dataCriacao(LocalDateTime.now())
                .build();

        contratoRepository.save(contrato);

        return mapper.toResponseDto(contrato);
    }

    @Override
    @Transactional
    public ContratoResponseDTO atualizarStatus(UUID id, Long statusId) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new ContratoNotFoundException("Contrato com o id " + id + " não foi encontrado"));

        StatusContrato novoStatus = statusContratoRepository.findById(statusId)
                .orElseThrow(() -> new StatusContratoNotFoundException("Status com o id " + statusId + " não foi encontrado"));

        contrato.setStatus(novoStatus);
        contrato.setDataAtualizacaoStatus(LocalDateTime.now());
        contratoRepository.save(contrato);

        return mapper.toResponseDto(contrato);
    }
}
