package com.mypetadmin.ps_contrato.controller;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.dto.ContratoStatusUpdateDTO;
import com.mypetadmin.ps_contrato.dto.PagamentoConfirmadoRequestDTO;
import com.mypetadmin.ps_contrato.enums.DirectionField;
import com.mypetadmin.ps_contrato.enums.SortField;
import com.mypetadmin.ps_contrato.service.ContratoService;
import com.mypetadmin.ps_contrato.service.TenantContratoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
@Validated
public class ContratoController {

    private static final String ACTOR_EMPRESA_ID_HEADER = "X-Actor-Empresa-Id";

    private final ContratoService contratoService;
    private final TenantContratoQueryService tenantContratoQueryService;

    @PostMapping
    @Operation(summary = "Cria contrato após conclusão do onboarding")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contrato criado ou replay idempotente concluído com sucesso."),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada."),
            @ApiResponse(responseCode = "409", description = "Conflito de contrato ou onboarding."),
            @ApiResponse(responseCode = "502", description = "Falha de integração com PS_Empresa.")
    })
    public ResponseEntity<ContratoResponseDTO> criarContrato(@Valid @RequestBody ContratoRequestDTO request) {
        ContratoResponseDTO contrato = contratoService.criarContrato(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(contrato.getId())
                .toUri();
        return ResponseEntity.created(location).body(contrato);
    }

    @PostMapping("/{id}/pagamentos/confirmacao")
    @Operation(summary = "Confirma pagamento e ativa o contrato quando elegível")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento confirmado ou replay idempotente processado."),
            @ApiResponse(responseCode = "404", description = "Contrato ou status não encontrado."),
            @ApiResponse(responseCode = "409", description = "Confirmação de pagamento incompatível com o contrato."),
            @ApiResponse(responseCode = "502", description = "Falha de integração com PS_Empresa.")
    })
    public ResponseEntity<ContratoResponseDTO> confirmarPagamento(
            @PathVariable UUID id,
            @Valid @RequestBody PagamentoConfirmadoRequestDTO request) {
        return ResponseEntity.ok(contratoService.confirmarPagamento(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza administrativamente o status do contrato; ativação depende de pagamento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Contrato ou status não encontrado."),
            @ApiResponse(responseCode = "409", description = "Transição administrativa inválida."),
            @ApiResponse(responseCode = "502", description = "Falha de integração com PS_Empresa.")
    })
    public ResponseEntity<ContratoResponseDTO> atualizarStatus(@PathVariable UUID id,
                                                               @Valid @RequestBody ContratoStatusUpdateDTO request) {
        return ResponseEntity.ok(contratoService.atualizarStatus(id, request.getStatusId()));
    }

    @GetMapping("/tenant")
    @Operation(summary = "Consulta contratos restritos ao tenant autenticado na borda")
    public ResponseEntity<Page<ContratoResponseDTO>> buscarContratosDoTenant(
            @RequestHeader(ACTOR_EMPRESA_ID_HEADER) UUID actorEmpresaId,
            @RequestParam(required = false) String numeroContrato,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "DATA_CRIACAO") SortField sortField,
            @RequestParam(defaultValue = "DESC") DirectionField direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction.getDirection(), sortField.getField()));
        return ResponseEntity.ok(tenantContratoQueryService.buscar(
                actorEmpresaId,
                numeroContrato,
                status,
                dataInicio,
                dataFim,
                pageable
        ));
    }

    @GetMapping
    @Operation(summary = "Busca administrativa de contratos com filtros, ordenação e paginação")
    public ResponseEntity<Page<ContratoResponseDTO>> buscarContratos(
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) String numeroContrato,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "DATA_CRIACAO") SortField sortField,
            @RequestParam(defaultValue = "DESC") DirectionField direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction.getDirection(), sortField.getField()));
        Page<ContratoResponseDTO> contratos = contratoService.buscarContratos(
                empresaId,
                numeroContrato,
                status,
                dataInicio,
                dataFim,
                pageable
        );

        return ResponseEntity.ok(contratos);
    }
}
