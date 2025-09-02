package com.mypetadmin.ps_contrato.repository;

import com.mypetadmin.ps_contrato.model.Contrato;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ContratoRepository extends JpaRepository<Contrato, UUID>, JpaSpecificationExecutor<Contrato> {

    Contrato findTopByContractNumberStartingWithOrderByContractNumberDesc(String prefixo);

    Optional<Contrato> findTopByEmpresaIdOrderByDataCriacaoDesc(@NotNull UUID empresaId);
}
