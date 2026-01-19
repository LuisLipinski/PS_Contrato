package com.mypetadmin.ps_contrato.enums;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectionFieldTest {

    @Test
    void deveMapearAsc() {
        assertThat(DirectionField.ASC.getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void deveMapearDesc() {
        assertThat(DirectionField.DESC.getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
