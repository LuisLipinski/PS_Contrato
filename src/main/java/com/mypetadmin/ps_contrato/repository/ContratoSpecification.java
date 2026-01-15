package com.mypetadmin.ps_contrato.repository;

import com.mypetadmin.ps_contrato.model.Contrato;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;


public class ContratoSpecification {

    public static Specification<Contrato> filtrar(
            UUID empresaId,
            String numeroContrato,
            String status,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (empresaId != null) {
                predicates.add(cb.equal(root.get("empresaId"), empresaId));
            }

            if (numeroContrato != null && !numeroContrato.isBlank()) {
                predicates.add(cb.equal(root.get("contractNumber"), numeroContrato));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.upper(root.get("status").get("statusName")),
                                status.toUpperCase()
                        )
                );
            }

            if (dataInicio != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("dataCriacao"),
                                dataInicio.atStartOfDay()
                        )
                );
            }

            if (dataFim != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("dataCriacao"),
                                dataFim.atTime(23,59,59)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

    }
}
