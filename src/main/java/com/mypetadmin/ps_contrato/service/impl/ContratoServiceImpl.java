package com.mypetadmin.ps_contrato.service.impl;

import com.mypetadmin.ps_contrato.client.EmpresaClient;
import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.exception.EmpresaNaoEncontradaException;
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
            String status = contratoExistente.getStatus().getStatusName();
            if (!status.equalsIgnoreCase("Inativo")) {
                throw new IllegalStateException("Já existe um contrato com status " + status + " para esta empresa");
            }
        }

        LocalDate hoje = LocalDate.now();
        int ano = hoje.getYear();
        int mes = hoje.getMonthValue();

        String prefixo = String.format("%04d%02d", ano, mes);

        Contrato ultimoContrato = contratoRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(prefixo);
        long sequencial = ultimoContrato == null ? 0 : Long.parseLong(ultimoContrato.getContractNumber().substring(6)) + 1;

        String numeroContrato = GerarNumeroContratoUtil.gerarNumeroContrato(sequencial);

        StatusContrato statusContrato = statusContratoRepository.findByStatusName("Aguardando pagamento")
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
}
