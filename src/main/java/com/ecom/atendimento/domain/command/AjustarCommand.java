package com.ecom.atendimento.domain.command;

import com.ecom.atendimento.domain.valueobject.*;
import com.ecom.atendimento.infrastructure.security.Authorizable;
import com.ecom.core.cqrs.domain.command.Command;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Comando para ajustar um atendimento existente.
 * Pode ser executado múltiplas vezes (estado SOLICITADO ou AJUSTADO → AJUSTADO).
 */
@Getter
@ToString(callSuper = true)
public class AjustarCommand extends Command implements Authorizable {

    private final String descricao;          // Opcional
    private final Prestador prestador;        // Opcional
    private final Servico servico;            // Opcional (pode alterar serviço)
    private final Endereco origem;            // Opcional (pode alterar origem)
    private final Endereco destino;           // Obrigatório quando prestador definido
    private final List<Item> items;           // Opcional (itens de cobrança)

    @JsonCreator
    public AjustarCommand(
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("descricao") String descricao,
            @JsonProperty("prestador") Prestador prestador,
            @JsonProperty("servico") Servico servico,
            @JsonProperty("origem") Endereco origem,
            @JsonProperty("destino") Endereco destino,
            @JsonProperty("items") List<Item> items) {

        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, AjustarCommand.class);

        if (aggregateId == null) {
            throw new IllegalArgumentException("AggregateId não pode ser nulo");
        }
        if (descricao != null && descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição não pode ser vazia se informada");
        }
        // Validação: se prestador informado, destino é obrigatório
        if (prestador != null && destino == null) {
            throw new IllegalArgumentException("Destino é obrigatório quando prestador é informado");
        }
        // Validação: se items informado, não pode ser vazio
        if (items != null && items.isEmpty()) {
            throw new IllegalArgumentException("Lista de items não pode ser vazia quando informada");
        }

        this.descricao = descricao != null ? descricao.trim() : null;
        this.prestador = prestador;
        this.servico = servico;
        this.origem = origem;
        this.destino = destino;
        this.items = items;
    }

    public String [] toWrite() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
    
    public String [] toRead() {
    	return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
}
