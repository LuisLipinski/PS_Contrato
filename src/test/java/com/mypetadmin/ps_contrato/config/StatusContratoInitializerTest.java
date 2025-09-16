package com.mypetadmin.ps_contrato.config;

import com.mypetadmin.ps_contrato.model.StatusContrato;
import com.mypetadmin.ps_contrato.repository.StatusContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class StatusContratoInitializerTest {

    private StatusContratoRepository repository;
    private StatusContratoInitializer initializer;

    @BeforeEach
    void setUp() {
        repository = mock(StatusContratoRepository.class);
        initializer = new StatusContratoInitializer();
    }

    @Test
    void deveSalvarStatusQueNaoExistem() throws Exception {
        // Simulando que nenhum status existe no banco
        when(repository.findByStatusName(anyString())).thenReturn(Optional.empty());

        initializer.initStatusContrato(repository).run(null);

        // Captura os objetos salvos
        ArgumentCaptor<StatusContrato> captor = ArgumentCaptor.forClass(StatusContrato.class);
        verify(repository, times(3)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(StatusContrato::getStatusName)
                .containsExactlyInAnyOrder("Aguardando pagamento", "Ativo", "Inativo");
    }

    @Test
    void naoDeveSalvarStatusSeJaExistir() throws Exception {
        // Simulando que todos já existem
        when(repository.findByStatusName(anyString()))
                .thenReturn(Optional.of(new StatusContrato()));

        initializer.initStatusContrato(repository).run(null);

        // Nunca deve salvar, pois já existem
        verify(repository, never()).save(any(StatusContrato.class));
    }

    @Test
    void deveSalvarSomenteStatusFaltantes() throws Exception {
        // "Ativo" já existe, os outros não
        when(repository.findByStatusName("Aguardando pagamento")).thenReturn(Optional.empty());
        when(repository.findByStatusName("Ativo")).thenReturn(Optional.of(new StatusContrato()));
        when(repository.findByStatusName("Inativo")).thenReturn(Optional.empty());

        initializer.initStatusContrato(repository).run(null);

        ArgumentCaptor<StatusContrato> captor = ArgumentCaptor.forClass(StatusContrato.class);
        verify(repository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(StatusContrato::getStatusName)
                .containsExactlyInAnyOrder("Aguardando pagamento", "Inativo");
    }
}
