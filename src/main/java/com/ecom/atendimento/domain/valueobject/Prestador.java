package com.ecom.atendimento.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representando um Prestador de Serviço.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Prestador implements Serializable {

    private static final long serialVersionUID = -5591316243584363938L;
	private final Long id;
    private final String nome;
    private final DocFiscal docfiscal;

    @JsonCreator
    public Prestador(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome,
            @JsonProperty("docfiscal") DocFiscal docfiscal) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID do prestador deve ser maior que zero");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do prestador não pode ser vazio");
        }
        if (nome.length() > 200) {
            throw new IllegalArgumentException("Nome do prestador não pode ter mais de 200 caracteres");
        }
        if (docfiscal == null) {
            throw new IllegalArgumentException("Documento fiscal do prestador não pode ser nulo");
        }
        this.id = id;
        this.nome = nome.trim();
        this.docfiscal = docfiscal;
    }
}
