package com.mypetadmin.ps_contrato.controller;

import com.mypetadmin.ps_contrato.dto.ContratoRequestDTO;
import com.mypetadmin.ps_contrato.dto.ContratoResponseDTO;
import com.mypetadmin.ps_contrato.service.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
@Validated
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping("/criarContrato")
    public ResponseEntity<ContratoResponseDTO> criarContrato(@Valid @RequestBody ContratoRequestDTO contratoRequestDTO) {
        ContratoResponseDTO contratoResponseDTO = contratoService.criarContrato(contratoRequestDTO);
        return  new ResponseEntity<>(contratoResponseDTO, HttpStatus.CREATED);
    }
}
