package com.mypetadmin.ps_contrato.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "contract_number", nullable = false, unique = true)
    private String contractNumber;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private StatusContrato status;

    @Builder.Default
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_atualizacao_status")
    private LocalDateTime dataAtualizacaoStatus;
}
