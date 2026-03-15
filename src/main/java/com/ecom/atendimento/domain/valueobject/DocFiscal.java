package com.ecom.atendimento.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representando um Documento Fiscal (CPF ou CNPJ).
 */
@Getter
@EqualsAndHashCode
@ToString
public class DocFiscal implements Serializable {

    private static final long serialVersionUID = 2650967156008879268L;
	private final String tipo;    // CPF ou CNPJ
    private final String doc;  // Número sem formatação

    @JsonCreator
    public DocFiscal(
            @JsonProperty("tipo") String tipo,
            @JsonProperty("doc") String doc) {
        if (tipo == null || tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo do documento fiscal não pode ser vazio");
        }
        if (doc == null || doc.trim().isEmpty()) {
            throw new IllegalArgumentException("Número do documento fiscal não pode ser vazio");
        }
        this.tipo = tipo.toUpperCase();
        this.doc = doc.replaceAll("[^0-9]", ""); // Remove formatação
    }
}
