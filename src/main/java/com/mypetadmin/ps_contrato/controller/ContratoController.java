package com.mypetadmin.ps_contrato.controller;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.ContratoStatusUpdateDTO;
import com.mypetadmin.ps_contrato.service.ContratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
@Validated
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping("/criarContrato")
    @Operation(summary = "Gera um novo contrato para uma empresa com status AGUARDANDO PAGAMENTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contrato criado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada."),
            @ApiResponse(responseCode = "400", description = "Dados invalidos.")
    })
    public ResponseEntity<ContratoResponseDTO> criarContrato(@Valid @RequestBody ContratoRequestDTO contratoRequestDTO) {
        ContratoResponseDTO contratoResponseDTO = contratoService.criarContrato(contratoRequestDTO);
        return  new ResponseEntity<>(contratoResponseDTO, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Atualiza o status do contrato")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado."),
            @ApiResponse(responseCode = "400", description = "Status invalido.")
    })
    public ResponseEntity<ContratoResponseDTO>atualizarStatus(@PathVariable UUID id,
                                                              @Valid @RequestBody ContratoStatusUpdateDTO statusUpdateDTO) {
        ContratoResponseDTO contratoAtualizado = contratoService.atualizarStatus(id, statusUpdateDTO.getStatusId());
        return ResponseEntity.ok(contratoAtualizado);
    }

}
