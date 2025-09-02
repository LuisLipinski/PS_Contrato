package com.mypetadmin.ps_contrato.config;

import com.mypetadmin.ps_contrato.model.StatusContrato;
import com.mypetadmin.ps_contrato.repository.StatusContratoRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class StatusContratoInitializer {


    @Bean
    public ApplicationRunner initStatusContrato(StatusContratoRepository repository) {
        return args -> {
            List<String> statusList = List.of("Aguardando pagamento", "Ativo", "Inativo");

            for (String status : statusList) {
                repository.findByStatusName(status).orElseGet(() -> {
                    StatusContrato sc = new StatusContrato();
                    sc.setStatusName(status);
                    sc.setDescricao(status); // ou alguma descrição mais detalhada
                    return repository.save(sc);
                });
            }
        };
    }
}
