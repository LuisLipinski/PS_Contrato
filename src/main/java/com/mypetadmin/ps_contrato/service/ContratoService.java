package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ContratoService {
    ContratoResponseDTO criarContrato(@Valid ContratoRequestDTO contratoRequestDTO);

    ContratoResponseDTO atualizarStatus(UUID id, @NotNull(message = "O Status não pode ser nulo") Long statusId);

    Page<ContratoResponseDTO> buscarContratos(UUID empresaId, String numeroContrato, String status, LocalDate dataInicio, LocalDate dataFim, Pageable pageable);
}
