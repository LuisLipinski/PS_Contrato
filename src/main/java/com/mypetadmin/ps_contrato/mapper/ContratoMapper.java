package com.mypetadmin.ps_contrato.mapper;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.model.Contrato;
import com.mypetadmin.ps_contrato.model.StatusContrato;
import org.springframework.stereotype.Component;

@Component
public class ContratoMapper {

    public Contrato toEntity(ContratoRequestDTO dto, StatusContrato status) {
        if (dto == null) return null;

        return Contrato.builder()
                .empresaId(dto.getEmpresaId())
                .status(status)
                .build();
    }

    public ContratoResponseDTO toResponseDto(Contrato contrato) {
        if (contrato == null) return null;

        return ContratoResponseDTO.builder()
                .id(contrato.getId())
                .empresaId(contrato.getEmpresaId())
                .numeroContrato(contrato.getContractNumber())
                .statusName(contrato.getStatus() != null ? contrato.getStatus().getStatusName() : null)
                .dataCriacao(contrato.getDataCriacao())
                .dataAtualizacaoStatus(contrato.getDataAtualizacaoStatus())
                .build();
    }
}
