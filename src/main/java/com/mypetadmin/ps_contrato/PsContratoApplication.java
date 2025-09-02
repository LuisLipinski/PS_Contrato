package com.mypetadmin.ps_contrato;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.mypetadmin.ps_contrato.client")
public class PsContratoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PsContratoApplication.class, args);
	}

}
