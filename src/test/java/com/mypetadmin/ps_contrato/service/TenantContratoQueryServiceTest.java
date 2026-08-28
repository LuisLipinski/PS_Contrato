package com.mypetadmin.ps_contrato.service;

import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantContratoQueryServiceTest {

    @Mock
    private ContratoService contratoService;

    @Test
    void deveForcarEmpresaDoAtorNaConsulta() {
        UUID actorEmpresaId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        TenantContratoQueryService service = new TenantContratoQueryService(contratoService);
        when(contratoService.buscarContratos(
                eq(actorEmpresaId),
                eq("202608"),
                eq("Ativo"),
                eq(LocalDate.of(2026, 8, 1)),
                eq(LocalDate.of(2026, 8, 31)),
                eq(pageable)))
                .thenReturn(Page.empty());

        Page<ContratoResponseDTO> result = service.buscar(
                actorEmpresaId,
                "202608",
                "Ativo",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                pageable);

        assertThat(result).isEmpty();
        verify(contratoService).buscarContratos(
                actorEmpresaId,
                "202608",
                "Ativo",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                pageable);
    }

    @Test
    void deveRejeitarAusenciaDoContextoDeTenant() {
        TenantContratoQueryService service = new TenantContratoQueryService(contratoService);
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.buscar(null, null, null, null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empresa");

        verify(contratoService, never()).buscarContratos(null, null, null, null, null, pageable);
    }
}
