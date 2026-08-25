package com.mypetadmin.ps_contrato.client;

import com.mypetadmin.ps_contrato.config.InternalFeignConfig;
import com.mypetadmin.ps_contrato.dto.EmpresaContratoStatusDTO;
import com.mypetadmin.ps_contrato.dto.EmpresaStatusResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "ps-empresa",
        url = "${clients.ps-empresa.url}",
        configuration = InternalFeignConfig.class
)
public interface EmpresaClient {

    @GetMapping("/internal/empresas/{id}/status")
    EmpresaStatusResponseDTO buscarStatusEmpresa(@PathVariable("id") UUID id);

    @PatchMapping("/internal/contratos/status")
    void sincronizarStatusContrato(@RequestBody EmpresaContratoStatusDTO dto);
}
