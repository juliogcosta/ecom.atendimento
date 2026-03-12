package com.ecom.atendimento.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representando um Veículo.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Veiculo implements Serializable {

    private static final long serialVersionUID = 8858382787102387819L;
	private final String placa;

    @JsonCreator
    public Veiculo(@JsonProperty("placa") String placa) {
        if (placa == null || placa.trim().isEmpty()) {
            throw new IllegalArgumentException("Placa do veículo não pode ser vazia");
        }
        if (placa.length() > 10) {
            throw new IllegalArgumentException("Placa do veículo não pode ter mais de 10 caracteres");
        }
        this.placa = placa.trim().toUpperCase();
    }
}
