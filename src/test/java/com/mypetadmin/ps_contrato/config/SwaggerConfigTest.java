package com.mypetadmin.ps_contrato.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerConfigTest {
    private SwaggerConfig swaggerConfig;

    @BeforeEach
    void setUp() {
        swaggerConfig = new SwaggerConfig();
    }

    @Test
    void deveCriarBeanCustomOpenAPIComInformacoesCorretas() {
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        Info info = openAPI.getInfo();

        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("My pet Admin - PS_Contrato");
        assertThat(info.getVersion()).isEqualTo("v1.0.0");
        assertThat(info.getDescription()).contains("MS responsavel para gestão dos contratos");

        Contact contact = info.getContact();
        assertThat(contact).isNotNull();
        assertThat(contact.getName()).isEqualTo("Equipe MyPetAdmin");
        assertThat(contact.getEmail()).isEqualTo("lhlipinski@gmail.com");
        assertThat(contact.getUrl()).isEqualTo("https://localhost:3000/login");

        License license = info.getLicense();
        assertThat(license).isNotNull();
        assertThat(license.getName()).isEqualTo("Apache 2.0");
        assertThat(license.getUrl()).isEqualTo("http://springdoc.org");
    }

    @Test
    void deveCriarBeanEmpresaGroupComCaminhoCorreto() {
        GroupedOpenApi groupedOpenApi = swaggerConfig.empresaGroup();

        assertThat(groupedOpenApi).isNotNull();
        assertThat(groupedOpenApi.getGroup()).isEqualTo("Contratos");
        assertThat(groupedOpenApi.getPathsToMatch()).containsExactly("/contratos/**");
    }
}
