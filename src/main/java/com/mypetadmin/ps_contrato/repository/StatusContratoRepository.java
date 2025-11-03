package com.mypetadmin.ps_contrato.repository;

import com.mypetadmin.ps_contrato.model.Contrato;
import com.mypetadmin.ps_contrato.model.StatusContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StatusContratoRepository extends JpaRepository<StatusContrato, Long>, JpaSpecificationExecutor<Contrato> {
    Optional<StatusContrato> findByStatusName(String aguardandoPagamento);
}
