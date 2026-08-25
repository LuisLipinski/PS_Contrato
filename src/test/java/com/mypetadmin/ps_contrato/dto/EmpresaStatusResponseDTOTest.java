package com.mypetadmin.ps_contrato.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmpresaStatusResponseDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveDesserializarContratoInternoDoPsEmpresa() throws Exception {
        UUID empresaId = UUID.randomUUID();
        String json = "{\"empresaId\":\"" + empresaId + "\",\"status\":\"AGUARDANDO_CONTRATO\"}";

        EmpresaStatusResponseDTO response = objectMapper.readValue(json, EmpresaStatusResponseDTO.class);

        assertThat(response.empresaId()).isEqualTo(empresaId);
        assertThat(response.status()).isEqualTo("AGUARDANDO_CONTRATO");
    }
}
