package com.mypetadmin.ps_contrato.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoResponseDTO {

    private UUID id;
    private UUID empresaId;
    private String numeroContrato;
    private String statusName;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacaoStatus;
}
