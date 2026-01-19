package com.mypetadmin.ps_contrato.mapper;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.model.Contrato;
import com.mypetadmin.ps_contrato.model.StatusContrato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ContratoMapperTest {

    private ContratoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContratoMapper();
    }

    @Test
    void toEntity_deveConverterRequestParaContrato() {
        UUID empresaId = UUID.randomUUID();
        ContratoRequestDTO dto = ContratoRequestDTO.builder()
                .empresaId(empresaId)
                .build();

        StatusContrato status = StatusContrato.builder()
                .statusName("Aguardando pagamento")
                .build();

        Contrato contrato = mapper.toEntity(dto, status);

        assertThat(contrato).isNotNull();
        assertThat(contrato.getEmpresaId()).isEqualTo(empresaId);
        assertThat(contrato.getStatus()).isEqualTo(status);
    }

    @Test
    void toEntity_quandoDtoForNull_retornaNull() {
        Contrato contrato = mapper.toEntity(null, null);
        assertThat(contrato).isNull();
    }

    @Test
    void toResponseDto_deveConverterContratoParaResponse() {
        UUID id = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        LocalDateTime dataCriacao = LocalDateTime.now();
        LocalDateTime dataAtualizacao = LocalDateTime.now();

        StatusContrato status = StatusContrato.builder()
                .statusName("Ativo")
                .build();

        Contrato contrato = Contrato.builder()
                .id(id)
                .empresaId(empresaId)
                .contractNumber("CT-12345")
                .status(status)
                .dataCriacao(dataCriacao)
                .dataAtualizacaoStatus(dataAtualizacao)
                .build();

        ContratoResponseDTO response = mapper.toResponseDto(contrato);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEmpresaId()).isEqualTo(empresaId);
        assertThat(response.getNumeroContrato()).isEqualTo("CT-12345");
        assertThat(response.getStatusName()).isEqualTo("Ativo");
        assertThat(response.getDataCriacao()).isEqualTo(dataCriacao);
        assertThat(response.getDataAtualizacaoStatus()).isEqualTo(dataAtualizacao);
    }

    @Test
    void toResponseDto_quandoContratoForNull_retornaNull() {
        ContratoResponseDTO response = mapper.toResponseDto(null);
        assertThat(response).isNull();
    }

    @Test
    void toResponseDto_quandoStatusForNull_retornaStatusNameNull() {
        Contrato contrato = Contrato.builder()
                .id(UUID.randomUUID())
                .empresaId(UUID.randomUUID())
                .contractNumber("CT-99999")
                .status(null) // <-- status nulo
                .dataCriacao(LocalDateTime.now())
                .build();

        ContratoResponseDTO response = mapper.toResponseDto(contrato);

        assertThat(response).isNotNull();
        assertThat(response.getStatusName()).isNull();
    }
}
