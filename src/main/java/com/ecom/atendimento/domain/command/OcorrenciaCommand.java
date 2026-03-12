package com.ecom.atendimento.domain.command;

import com.ecom.atendimento.infrastructure.security.Authorizable;
import com.ecom.core.cqrs.domain.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Comando para registrar ocorrências durante a execução do atendimento.
 * Pode ser executado múltiplas vezes (estado CONFIRMADO ou OCORRIDO → OCORRIDO).
 *
 * NOVO: Este comando não existe na implementação de referência.
 */
@Getter
@ToString(callSuper = true)
public class OcorrenciaCommand extends Command implements Authorizable {

    private final List<String> ocorrencias;

    @JsonCreator
    public OcorrenciaCommand(
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("ocorrencias") List<String> ocorrencias) {

        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, OcorrenciaCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
        if (ocorrencias == null || ocorrencias.isEmpty()) {
            throw new IllegalArgumentException("Lista de ocorrências não pode ser vazia");
        }
        // Valida cada ocorrência
        for (String ocorrencia : ocorrencias) {
            if (ocorrencia == null || ocorrencia.trim().isEmpty()) {
                throw new IllegalArgumentException("Ocorrência não pode ser vazia");
            }
        }

        this.ocorrencias = ocorrencias.stream()
                .map(String::trim)
                .toList();
    }

    public String [] toWrite() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
    
    public String [] toRead() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
}
