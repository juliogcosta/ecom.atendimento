package com.ecom.atendimento.domain.command;

import com.ecom.atendimento.infrastructure.security.Authorizable;
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
public class CancelarCommand extends Command implements Authorizable {

    @JsonCreator
    public CancelarCommand(@JsonProperty("aggregateId") UUID aggregateId) {
        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, CancelarCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
    }

    public String [] toWrite() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
    
    public String [] toRead() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
}
