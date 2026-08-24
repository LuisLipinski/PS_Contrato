package com.mypetadmin.ps_contrato.dto;

import jakarta.validation.constraints.NotNull;
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
public class PagamentoConfirmadoRequestDTO {

    @NotNull
    private UUID paymentId;

    @NotNull
    private LocalDateTime paidAt;
}
