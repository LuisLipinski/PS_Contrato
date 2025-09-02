package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import jakarta.validation.Valid;

public interface ContratoService {
    ContratoResponseDTO criarContrato(@Valid ContratoRequestDTO contratoRequestDTO);
}
