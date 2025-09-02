package com.mypetadmin.ps_contrato.client;


import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ps-empresa", url = "http://localhost:8081/empresas")
public interface EmpresaClient {

    @GetMapping("/buscaEmpresas/{id}")
    JsonNode existsById(@PathVariable("id") UUID id);
}
