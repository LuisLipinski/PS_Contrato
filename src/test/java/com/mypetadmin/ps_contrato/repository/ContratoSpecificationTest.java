package com.mypetadmin.ps_contrato.repository;

import com.mypetadmin.ps_contrato.model.Contrato;
import com.mypetadmin.ps_contrato.model.StatusContrato;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ContratoSpecificationTest {

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private StatusContratoRepository statusContratoRepository;

    private Contrato criarContrato(UUID empresaId, String numero, String status, LocalDateTime data) {
        StatusContrato statusContrato = statusContratoRepository.findByStatusName(status)
                .orElseGet(() ->
                        statusContratoRepository.save(
                                StatusContrato.builder()
                                        .statusName(status)
                                        .descricao(status)
                                        .build()
                        ));

        return contratoRepository.save(Contrato.builder()
                .empresaId(empresaId)
                .contractNumber(numero)
                .status(statusContrato)
                .dataCriacao(data)
                .build()
        );

    }

    @Test
    void filtrar_semFiltros_deveRetornarTodos() {
        criarContrato(UUID.randomUUID(), "CT-1", "Ativo", LocalDateTime.now());
        criarContrato(UUID.randomUUID(), "CT-2", "Inativo", LocalDateTime.now());

        Specification<Contrato> spec = ContratoSpecification.filtrar(null,null,null,null,null);

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void filtrar_porEmpresaId() {
        UUID empresaId = UUID.randomUUID();
        criarContrato(empresaId, "CT-1", "Ativo", LocalDateTime.now());
        criarContrato(UUID.randomUUID(), "CT-2", "Ativo", LocalDateTime.now());
        Specification<Contrato> spec = ContratoSpecification.filtrar(empresaId,null,null,null,null);

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado)
                .hasSize(1)
                .first()
                .extracting(Contrato::getEmpresaId)
                .isEqualTo(empresaId);
    }

    @Test
    void filtrar_porNumeroContrato() {
        criarContrato(UUID.randomUUID(), "CT-123", "Ativo", LocalDateTime.now());
        criarContrato(UUID.randomUUID(), "CT-999", "Ativo", LocalDateTime.now());

        Specification<Contrato> spec = ContratoSpecification.filtrar(null,"CT-123",null,null,null);

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado)
                .hasSize(1)
                .first()
                .extracting(Contrato::getContractNumber)
                .isEqualTo("CT-123");

    }

    @Test
    void filtrar_porStatus_caseInsensitive() {
        criarContrato(UUID.randomUUID(), "CT-1", "Ativo", LocalDateTime.now());
        criarContrato(UUID.randomUUID(), "CT-2", "Inativo", LocalDateTime.now());

        Specification<Contrato> spec = ContratoSpecification.filtrar(null,null, "ativo", null, null);

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado).hasSize(1)
                .first()
                .extracting(c -> c.getStatus().getStatusName())
                .isEqualTo("Ativo");
    }

    @Test
    void filtrar_porDataInicio() {
        criarContrato(UUID.randomUUID(), "CT-OLD", "Ativo", LocalDateTime.of(2025,12,31,10,0));
        criarContrato(UUID.randomUUID(), "CT-NEW", "Ativo", LocalDateTime.of(2026,1,5,10,0));

        Specification<Contrato> spec = ContratoSpecification.filtrar(null, null, null, java.time.LocalDate.of(2026,1,1), null);

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado)
                .hasSize(1)
                .first()
                .extracting(Contrato::getContractNumber)
                .isEqualTo("CT-NEW");
    }

    @Test
    void filtrar_porDataFim() {
        criarContrato(UUID.randomUUID(), "CT-1", "Ativo",
                LocalDateTime.of(2026, 1, 10, 10, 0));
        criarContrato(UUID.randomUUID(), "CT-2", "Ativo",
                LocalDateTime.of(2026, 2, 10, 10, 0));

        Specification<Contrato> spec = ContratoSpecification.filtrar(
                null, null, null,
                null,
                java.time.LocalDate.of(2026, 1, 31)
        );

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado)
                .hasSize(1)
                .first()
                .extracting(Contrato::getContractNumber)
                .isEqualTo("CT-1");
    }

    @Test
    void filtrar_combinandoFiltros() {
        UUID empresaId = UUID.randomUUID();

        criarContrato(empresaId, "CT-OK", "Ativo",
                LocalDateTime.of(2026, 1, 15, 12, 0));

        criarContrato(empresaId, "CT-FAIL", "Inativo",
                LocalDateTime.of(2026, 1, 15, 12, 0));

        Specification<Contrato> spec = ContratoSpecification.filtrar(
                empresaId,
                "CT-OK",
                "ATIVO",
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 1, 31)
        );

        List<Contrato> resultado = contratoRepository.findAll(spec);

        assertThat(resultado)
                .hasSize(1)
                .first()
                .extracting(Contrato::getContractNumber)
                .isEqualTo("CT-OK");
    }

}
