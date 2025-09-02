package com.mypetadmin.ps_contrato.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;

public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("My pet Admin - PS_Contrato")
                        .version("v1.0.0")
                        .description("MS responsavel para gestão dos contratos e vincular as empresas do My Pet Admin")
                        .contact(new Contact()
                                .name("Equipe MyPetAdmin")
                                .email("lhlipinski@gmail.com")
                                .url("https://localhost:3000/login"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }

    @Bean
    public GroupedOpenApi empresaGroup() {
        return GroupedOpenApi.builder()
                .group("Contratos")
                .pathsToMatch("/contratos/**")
                .build();
    }
}
