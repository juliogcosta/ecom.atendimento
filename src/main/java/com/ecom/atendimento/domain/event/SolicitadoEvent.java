package com.ecom.atendimento.domain.event;

import com.ecom.atendimento.domain.valueobject.*;
import com.ecom.core.cqrs.domain.event.Event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Evento representando que um atendimento foi solicitado.
 */
@Getter
@ToString(callSuper = true)
public class SolicitadoEvent extends Event {

    private final Status status;
    private final String protocolo;
    private final String tipodeocorrencia;
    private final Cliente cliente;
    private final Veiculo veiculo;
    private final Servico servico;
    private final Endereco base;
    private final Endereco origem;

    @JsonCreator
    public SolicitadoEvent(
            @JsonProperty("aggregateid") UUID aggregateid,
            @JsonProperty("version") int version,
            @JsonProperty("status") Status status,
            @JsonProperty("protocolo") String protocolo,
            @JsonProperty("tipodeocorrencia") String tipodeocorrencia,
            @JsonProperty("cliente") Cliente cliente,
            @JsonProperty("veiculo") Veiculo veiculo,
            @JsonProperty("servico") Servico servico,
            @JsonProperty("base") Endereco base,
            @JsonProperty("origem") Endereco origem) {

        super(aggregateid, version);
        this.status = status;
        this.protocolo = protocolo;
        this.tipodeocorrencia = tipodeocorrencia;
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.servico = servico;
        this.base = base;
        this.origem = origem;
    }

    @Override
    public String getEventType() {
        return "YC_ECOMIGO_ATENDIMENTO_SOLICITADO";
    }
}
