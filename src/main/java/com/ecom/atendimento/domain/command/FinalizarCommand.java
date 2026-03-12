package com.ecom.atendimento.domain.command;

import com.ecom.core.cqrs.domain.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Comando para finalizar um atendimento.
 * Transição: CONFIRMADO ou OCORRIDO → FINALIZADO
 *
 * IMPORTANTE: Este evento aciona integração com financeiro.atendimento
 */
@Getter
@ToString(callSuper = true)
public class FinalizarCommand extends Command {

    @JsonCreator
    public FinalizarCommand(@JsonProperty("aggregateId") UUID aggregateId) {
        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, FinalizarCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
    }
}
