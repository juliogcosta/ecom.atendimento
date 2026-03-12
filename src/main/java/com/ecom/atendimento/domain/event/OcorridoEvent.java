package com.ecom.atendimento.domain.event;

import com.ecom.atendimento.domain.valueobject.Status;
import com.ecom.core.cqrs.domain.event.Event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Evento representando que ocorrências foram registradas durante o atendimento.
 *
 * NOVO: Este evento não existe na implementação de referência.
 */
@Getter
@ToString(callSuper = true)
public class OcorridoEvent extends Event {

    private final Status status;
    private final List<String> ocorrencias;

    @JsonCreator
    public OcorridoEvent(
            @JsonProperty("aggregateid") UUID aggregateid,
            @JsonProperty("version") int version,
            @JsonProperty("status") Status status,
            @JsonProperty("ocorrencias") List<String> ocorrencias) {

        super(aggregateid, version);
        this.status = status;
        this.ocorrencias = ocorrencias;
    }

    @Override
    public String getEventType() {
        return "YC_ECOMIGO_ATENDIMENTO_OCORRIDO";
    }
}
