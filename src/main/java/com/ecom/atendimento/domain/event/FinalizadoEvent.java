package com.ecom.atendimento.domain.event;

import com.ecom.atendimento.domain.valueobject.Status;
import com.ecom.core.cqrs.domain.event.Event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Evento representando que um atendimento foi finalizado.
 *
 * IMPORTANTE: Este evento aciona integração com financeiro.atendimento.
 */
@Getter
@ToString(callSuper = true)
public class FinalizadoEvent extends Event {

    private final Status status;

    @JsonCreator
    public FinalizadoEvent(
            @JsonProperty("aggregateid") UUID aggregateid,
            @JsonProperty("version") int version,
            @JsonProperty("status") Status status) {

        super(aggregateid, version);
        this.status = status;
    }

    @Override
    public String getEventType() {
        return "YC_ECOMIGO_ATENDIMENTO_FINALIZADO";
    }
}
