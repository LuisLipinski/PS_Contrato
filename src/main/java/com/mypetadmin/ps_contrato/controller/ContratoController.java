package com.mypetadmin.ps_contrato.controller;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.ContratoStatusUpdateDTO;
import com.mypetadmin.ps_contrato.enums.DirectionField;
import com.mypetadmin.ps_contrato.enums.SortField;
import com.mypetadmin.ps_contrato.service.ContratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
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
        log.info(
                "Requisição para atualizar status do contrato {} para status {}",
                id,
                statusUpdateDTO.getStatusId()
        );
        ContratoResponseDTO contratoAtualizado = contratoService.atualizarStatus(id, statusUpdateDTO.getStatusId());
        return ResponseEntity.ok(contratoAtualizado);
    }

    @GetMapping
    @Operation(summary = "Busca contratos com filtros, ordenação e paginação")
    public ResponseEntity<Page<ContratoResponseDTO>> buscarContratos(@RequestParam(required = false) UUID empresaId,
                                                                     @RequestParam(required = false) String numeroContrato,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate dataInicio,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate dataFim,
                                                                     @RequestParam(defaultValue = "DATA_CRIACAO") SortField sortField,
                                                                     @RequestParam(defaultValue = "DESC") DirectionField direction,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size
                                                                     ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction.getDirection(), sortField.getField()));
        Page<ContratoResponseDTO> contratos =
                contratoService.buscarContratos(empresaId, numeroContrato, status, dataInicio, dataFim, pageable);

        return ResponseEntity.ok(contratoService.buscarContratos(
                empresaId,
                numeroContrato,
                status,
                dataInicio,
                dataFim,
                pageable
        ));
    }

    @Operation(summary = "Ativar empresa com base em contrato ativo (uso interno)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empresa ativada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Contrato não esta ativo")
    })
    @PutMapping("/internal/empresa/{empresaId}/ativar")
    public ResponseEntity<Void>ativarEmpresaPorContrato(@PathVariable @NotNull UUID empresaId) {
        log.info("Requisição interna para ativar empresa {} via contrato", empresaId);

        contratoService.ativarEmpresa(empresaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/empresa/{empresaId}/ativo")
    public ResponseEntity<Boolean> empresaPossuiContratoAtivo(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(contratoService.empresaPossuiContratoAtivo(empresaId));
    }

}
