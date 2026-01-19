package com.mypetadmin.ps_contrato.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContratoStatusUpdateDTO {
    @NotNull(message = "O Status não pode ser nulo")
    private Long statusId;
}
