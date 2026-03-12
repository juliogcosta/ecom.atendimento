package com.ecom.atendimento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Aplicação principal do microserviço de Atendimento.
 * Implementa CQRS/Event Sourcing para gerenciar agregados de Atendimento.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.ecom.atendimento",
    "com.ecom.core.cqrs",
    "com.ecom.core.common"
})
public class AssistenciaAtendimentoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistenciaAtendimentoServiceApplication.class, args);
    }
}
