package com.ecom.atendimento.domain.command;

import com.ecom.core.cqrs.domain.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Comando para cancelar um atendimento.
 * Transição: SOLICITADO ou AJUSTADO → CANCELADO
 *
 * RESTRIÇÃO: Não pode cancelar atendimento após confirmado.
 */
@Getter
@ToString(callSuper = true)
public class CancelarCommand extends Command {

    @JsonCreator
    public CancelarCommand(@JsonProperty("aggregateId") UUID aggregateId) {
        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, CancelarCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
    }
}
