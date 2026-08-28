package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantContratoQueryService {

    private final ContratoService contratoService;

    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> buscar(
            UUID actorEmpresaId,
            String numeroContrato,
            String status,
            LocalDate dataInicio,
            LocalDate dataFim,
            Pageable pageable) {
        if (actorEmpresaId == null) {
            throw new IllegalArgumentException("Contexto de empresa do ator é obrigatório");
        }

        return contratoService.buscarContratos(
                actorEmpresaId,
                numeroContrato,
                status,
                dataInicio,
                dataFim,
                pageable
        );
    }
}
