package com.mypetadmin.ps_contrato.enums;

import org.springframework.data.domain.Sort;

public enum DirectionField {

    ASC(Sort.Direction.ASC),
    DESC(Sort.Direction.DESC);

    private final Sort.Direction direction;

    DirectionField(Sort.Direction direction) {
        this.direction = direction;
    }

    public Sort.Direction getDirection() {
        return direction;
    }
}
