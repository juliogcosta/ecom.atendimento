package com.ecom.atendimento.domain.event;

import com.ecom.atendimento.domain.valueobject.*;
import com.ecom.core.cqrs.domain.event.Event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Evento representando que um atendimento foi ajustado.
 * Pode conter prestador, destino, itens, e/ou alterações em serviço/origem.
 */
@Getter
@ToString(callSuper = true)
public class AjustadoEvent extends Event {

    private final Status status;
    private final String descricao;
    private final Prestador prestador;
    private final Servico servico;
    private final Endereco origem;
    private final Endereco destino;
    private final List<Item> items;

    @JsonCreator
    public AjustadoEvent(
            @JsonProperty("aggregateid") UUID aggregateid,
            @JsonProperty("version") int version,
            @JsonProperty("status") Status status,
            @JsonProperty("descricao") String descricao,
            @JsonProperty("prestador") Prestador prestador,
            @JsonProperty("servico") Servico servico,
            @JsonProperty("origem") Endereco origem,
            @JsonProperty("destino") Endereco destino,
            @JsonProperty("items") List<Item> items) {

        super(aggregateid, version);
        this.status = status;
        this.descricao = descricao;
        this.prestador = prestador;
        this.servico = servico;
        this.origem = origem;
        this.destino = destino;
        this.items = items;
    }

    @Override
    public String getEventType() {
        return "YC_ECOMIGO_ATENDIMENTO_AJUSTADO";
    }
}
