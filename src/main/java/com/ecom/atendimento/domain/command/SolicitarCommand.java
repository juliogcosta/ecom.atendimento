package com.ecom.atendimento.domain.command;

import com.ecom.atendimento.domain.valueobject.*;
import com.ecom.atendimento.infrastructure.security.Authorizable;
import com.ecom.core.cqrs.domain.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Comando para solicitar um novo atendimento.
 * Representa a criação inicial do agregado Atendimento.
 */
@Getter
@ToString(callSuper = true)
public class SolicitarCommand extends Command implements Authorizable {

    private final String protocolo;
    private final String tipodeocorrencia;
    private final Cliente cliente;
    private final Veiculo veiculo;
    private final Servico servico;
    private final Endereco base;
    private final Endereco origem;

    @JsonCreator
    public SolicitarCommand(
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("protocolo") String protocolo,
            @JsonProperty("tipodeocorrencia") String tipodeocorrencia,
            @JsonProperty("cliente") Cliente cliente,
            @JsonProperty("veiculo") Veiculo veiculo,
            @JsonProperty("servico") Servico servico,
            @JsonProperty("base") Endereco base,
            @JsonProperty("origem") Endereco origem) {

        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, SolicitarCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
        if (protocolo == null || protocolo.trim().isEmpty()) {
            throw new IllegalArgumentException("Protocolo não pode ser vazio");
        }
        if (protocolo.length() > 64) {
            throw new IllegalArgumentException("Protocolo não pode ter mais de 64 caracteres");
        }
        if (tipodeocorrencia == null || tipodeocorrencia.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de ocorrência não pode ser vazio");
        }
        if (tipodeocorrencia.length() > 100) {
            throw new IllegalArgumentException("Tipo de ocorrência não pode ter mais de 100 caracteres");
        }
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não pode ser nulo");
        }
        if (veiculo == null) {
            throw new IllegalArgumentException("Veículo não pode ser nulo");
        }
        if (servico == null) {
            throw new IllegalArgumentException("Serviço não pode ser nulo");
        }
        if (base == null) {
            throw new IllegalArgumentException("Base não pode ser nula");
        }
        if (origem == null) {
            throw new IllegalArgumentException("Origem não pode ser nula");
        }

        this.protocolo = protocolo.trim();
        this.tipodeocorrencia = tipodeocorrencia.trim();
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.servico = servico;
        this.base = base;
        this.origem = origem;
    }

    public String [] toWrite() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
    
    public String [] toRead() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
}
