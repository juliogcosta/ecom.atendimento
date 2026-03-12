package com.ecom.atendimento.domain.command;

import com.ecom.core.cqrs.domain.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Comando para confirmar um atendimento.
 * Transição: SOLICITADO ou AJUSTADO → CONFIRMADO
 */
@Getter
@ToString(callSuper = true)
public class ConfirmarCommand extends Command {

    @JsonCreator
    public ConfirmarCommand(@JsonProperty("aggregateId") UUID aggregateId) {
        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, ConfirmarCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
    }
}
