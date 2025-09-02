package com.mypetadmin.ps_contrato.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "status_contrato")
public class StatusContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_status", nullable = false, unique = true)
    private String statusName;

    @Column(name = "descricao_status", nullable = false)
    private String descricao;
}
