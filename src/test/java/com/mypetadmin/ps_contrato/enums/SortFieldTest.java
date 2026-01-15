package com.mypetadmin.ps_contrato.enums;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

public class SortFieldTest {

    @Test
    void deveMapearCampoDataCriacao() {
        assertThat(SortField.DATA_CRIACAO.getField())
                .isEqualTo("dataCriacao");
    }

    @Test
    void devoMapearCampoNumeroContrato() {
        assertThat(SortField.NUMERO_CONTRATO.getField())
                .isEqualTo("contractNumber");
    }

    @Test
    void deveMapearCampoStatus() {
        assertThat(SortField.STATUS.getField())
                .isEqualTo("status.statusName");
    }
}
