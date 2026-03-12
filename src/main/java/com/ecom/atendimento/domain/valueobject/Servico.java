package com.ecom.atendimento.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representando um Serviço.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Servico implements Serializable {

    private static final long serialVersionUID = -1961970899098038568L;
	private final Long id;
    private final String nome;

    @JsonCreator
    public Servico(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID do serviço deve ser maior que zero");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do serviço não pode ser vazio");
        }
        if (nome.length() > 100) {
            throw new IllegalArgumentException("Nome do serviço não pode ter mais de 100 caracteres");
        }
        this.id = id;
        this.nome = nome.trim();
    }
}
